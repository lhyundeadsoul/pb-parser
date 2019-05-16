package me.lihongyu.utils.parser;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet.Field;
import me.lihongyu.utils.parser.syntax.StandardSyntax;
import me.lihongyu.utils.parser.syntax.Syntax;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Extract protobuf object from a protobuf string based on protobuf path specified,
 * and return string of the extracted protobuf object
 *
 * @author lihongyu
 * @date 2019/02/25
 */
public class DynamicPBParser {

    /**
     * all of Descriptor in desc file
     */
    private Map<String, Descriptor> descriptorCache;
    /**
     * all extension field in desc file
     */
    private Map<String, FieldDescriptor> extensionFieldCache;
    /**
     * field path string syntax
     */
    private Syntax syntax;

    private DynamicPBParser() {}

    public static ParserBuilder newBuilder() {
        return new ParserBuilder();
    }

    /**
     * parse all descriptors
     */
    private void load(String descFilePath) throws IOException {
        //just parse descriptor only once in one SELECT section
        if (descriptorCache != null && descriptorCache.size() > 0) {
            return;
        }

        try (InputStream resourceAsStream = FileUtils.openInputStream(FileUtils.getFile(descFilePath))) {
            //build file set
            FileDescriptorSet fileDescriptorSet = FileDescriptorSet.parseFrom(resourceAsStream);

            //cache file to build dependencies
            Map<String, FileDescriptorProto> fileProtoCache = fileDescriptorSet.getFileList().stream()
                .collect(Collectors.toMap(FileDescriptorProto::getName, Function.identity()));

            //iterate all proto file
            List<FileDescriptor> fileDescriptorList = fileDescriptorSet.getFileList().stream()
                .map(fileProto -> buildFileDescriptor(fileProto, fileProtoCache))
                .collect(Collectors.toList());

            //cache all message (descriptor)
            descriptorCache = fileDescriptorList.stream()
                .flatMap(fileDescriptor -> fileDescriptor.getMessageTypes().stream())
                .flatMap(descriptor -> {
                    //iterate all nested schema
                    List<Descriptor> nestedTypes = getAllNestedDescriptors(descriptor);
                    nestedTypes.add(descriptor);
                    return nestedTypes.stream();
                })
                .collect(Collectors.toMap(Descriptor::getFullName, Function.identity()));

            //cache all extension
            extensionFieldCache = fileDescriptorList.stream()
                .flatMap(fileDescriptor -> fileDescriptor.getExtensions().stream())
                .collect(Collectors.toMap(FieldDescriptor::getFullName, Function.identity()));
        }
    }

    /**
     * main logic
     *
     * @param base64Str   content of pb object encoded by base64
     * @param fullPathStr field full path
     * @return field value
     */
    public String parse(String base64Str, String fullPathStr) {
        //raw data must be nonnull and fullPathStr must have class$column pattern
        if (StringUtils.isBlank(base64Str) || !syntax.check(fullPathStr)) {
            return null;
        }

        String classFullName = syntax.getClassFullName(fullPathStr);
        String fieldPathStr = syntax.getFieldPathStr(fullPathStr);

        //find the specific descriptor with class full name(pkgname.classname)
        Descriptor descriptor = findDescriptor(classFullName);

        //parse object
        Object extractObj = parseObject(base64Str, descriptor);

        //get the field path
        String[] fieldPathArr = syntax.getFieldPathArr(fieldPathStr);

        //extract field value
        for (String fieldPath : fieldPathArr) {
            if (extractObj == null) {
                return null;
            }

            //parse field name and index
            String fieldName = syntax.getFieldName(fieldPath);
            String fieldIndex = syntax.getFieldIndex(fieldPath);
            FieldDescriptor fieldDescriptor = getFieldByName(descriptor, fieldName);

            //extract value with field name and index
            extractObj = extractWithField(extractObj, fieldDescriptor);
            if (extractObj instanceof List && fieldIndex != null && fieldIndex.length() > 0) {
                extractObj = extractWithIndex((List)extractObj, fieldIndex);
            }

            //update descriptor to field schema if field is an object that is used in next loop
            if (JavaType.MESSAGE.equals(fieldDescriptor.getJavaType())) {
                descriptor = fieldDescriptor.getMessageType();
            }
        }

        //output
        return assembleResult(extractObj);
    }

    /**
     * find normal/extension field by name
     *
     * @param descriptor
     * @param fieldName
     * @return
     */
    private FieldDescriptor getFieldByName(Descriptor descriptor, String fieldName) {
        FieldDescriptor fieldDescriptor = descriptor.findFieldByName(fieldName);
        if (fieldDescriptor == null) {
            fieldDescriptor = extensionFieldCache.get(fieldName);
        }
        if (fieldDescriptor == null){
            throw new IllegalArgumentException(fieldName + "is not found in "+ descriptor.getFullName());
        }
        return fieldDescriptor;
    }

    /**
     * extract field value with field name
     *
     * @param extractObj
     * @param fieldDescriptor
     * @return
     */
    private Object extractWithField(Object extractObj, FieldDescriptor fieldDescriptor) {
        if (extractObj instanceof List) {
            List<Object> result = new ArrayList<>();
            ((List<DynamicMessage>)extractObj).forEach(obj -> {
                Object fieldValue = getFieldValue(obj, fieldDescriptor);
                if (fieldValue instanceof List) {
                    result.addAll((List)fieldValue);
                } else if (fieldValue != null) {
                    result.add(fieldValue);
                }
            });
            return result;
        } else {
            return getFieldValue((DynamicMessage)extractObj, fieldDescriptor);
        }
    }

    /**
     * get field value even if it is an extension field
     *
     * @param extractObj
     * @param fieldDescriptor
     * @return
     */
    private Object getFieldValue(DynamicMessage extractObj, FieldDescriptor fieldDescriptor) {
        //normal field
        if (!fieldDescriptor.isExtension()) {
            // java.lang.IllegalArgumentException: hasField() can only be called on non-repeated fields.
            if (fieldDescriptor.isRepeated() || extractObj.hasField(fieldDescriptor)) {
                return extractObj.getField(fieldDescriptor);
            } else {
                return fieldDescriptor.hasDefaultValue() ? fieldDescriptor.getDefaultValue() : null;
            }
        }
        // extension field
        Object value = null;
        Field field = extractObj.getUnknownFields().getField(fieldDescriptor.getNumber());
        // extension field with basic type
        if (field.getVarintList() != null && field.getVarintList().size() > 0) {
            Long varint = field.getVarintList().get(0);
            if (JavaType.ENUM.equals(fieldDescriptor.getJavaType())) {
                value = fieldDescriptor.getEnumType().findValueByNumber(varint.intValue());
            } else if (JavaType.BOOLEAN.equals(fieldDescriptor.getJavaType())) {
                value = varint == 1 ? Boolean.TRUE : Boolean.FALSE;
            } else {
                // INT LONG
                value = varint;
            }
        } else if (field.getFixed32List() != null && field.getFixed32List().size() > 0) {
            Integer fix32 = field.getFixed32List().get(0);
            if (JavaType.FLOAT.equals(fieldDescriptor.getJavaType())) {
                value = Float.intBitsToFloat(fix32);
            }
        } else if (field.getFixed64List() != null && field.getFixed64List().size() > 0) {
            Long fixed64 = field.getFixed64List().get(0);
            if (JavaType.DOUBLE.equals(fieldDescriptor.getJavaType())) {
                value = Double.longBitsToDouble(fixed64);
            }
        } else if (field.getLengthDelimitedList() != null && field.getLengthDelimitedList().size() > 0) {
            // extension field with complex type
            ByteString bytes = field.getLengthDelimitedList().get(0);
            if (JavaType.MESSAGE.equals(fieldDescriptor.getJavaType())) {
                try {
                    value = DynamicMessage.parseFrom(fieldDescriptor.getMessageType(), bytes.toByteArray());
                } catch (InvalidProtocolBufferException e) {
                    return null;
                }
            } else if (JavaType.STRING.equals(fieldDescriptor.getJavaType())) {
                value = bytes.toStringUtf8();
            } else if (JavaType.BYTE_STRING.equals(fieldDescriptor.getJavaType())) {
                value = bytes;
            }
        }
        return value;
    }

    /**
     * extract List field value with field index
     *
     * @param extractObj
     * @param fieldIndex
     * @return null if index out of bounds
     */
    private Object extractWithIndex(List extractObj, String fieldIndex) {
        if (StringUtils.equals(syntax.getWildcard(), fieldIndex)) {
            return extractObj;
        }
        int index = Integer.parseInt(fieldIndex);
        return index >= extractObj.size() ? null : extractObj.get(index);
    }

    /**
     * find the descriptor by the full package.message path
     *
     * @param classFullName
     * @return
     */
    private Descriptor findDescriptor(String classFullName) {
        //user should notice that his class does not found and maybe it is just a slip of the pen
        if (!descriptorCache.containsKey(classFullName)) {
            throw new IllegalArgumentException(
                classFullName + " can not be found in any description file! Please check out if it exist.");
        }
        return descriptorCache.get(classFullName);
    }

    /**
     * get dynamic message
     *
     * @param base64Str
     * @param descriptor
     * @return
     */
    private Object parseObject(String base64Str, Descriptor descriptor) {
        Object extractObj;
        try {
            extractObj = DynamicMessage.parseFrom(descriptor, Base64.getDecoder().decode(base64Str));
        } catch (InvalidProtocolBufferException e) {
            //get_json_object return null even though the JSON string is broken, so following up on that
            //throw new IllegalStateException("parse dynamic message failed! base64:" + base64Str, e);
            return null;
        }
        return extractObj;
    }

    /**
     * get all nested types of a descriptor recursively
     *
     * @param descriptor
     * @return
     */
    private List<Descriptor> getAllNestedDescriptors(Descriptor descriptor) {
        List<Descriptor> nestedTypes = new ArrayList<>(descriptor.getNestedTypes());
        for (int i = 0; i < nestedTypes.size(); i++) {
            nestedTypes.addAll(getAllNestedDescriptors(nestedTypes.get(i)));
        }
        return nestedTypes;
    }

    /**
     * assemble the final result
     *
     * @param extractObj
     * @return
     */
    private String assembleResult(Object extractObj) {
        String result;
        if (extractObj instanceof List) {
            result = assembleListResult((List)extractObj);
        } else {
            result = assembleOneObject(extractObj);
        }
        return result;
    }

    /**
     * assemble just one object
     *
     * @param extractObj
     * @return
     */
    private String assembleOneObject(Object extractObj) {
        String result;
        if (extractObj == null) {
            result = null;
        } else if (extractObj instanceof DynamicMessage) {
            //return an object as a base64 string when field path is refer to an object
            result = Base64.getEncoder().encodeToString(((DynamicMessage)extractObj).toByteArray());
        } else if (extractObj instanceof ByteString) {
            result = Base64.getEncoder().encodeToString(((ByteString)extractObj).toByteArray());
        } else {
            result = String.valueOf(extractObj);
        }
        return result;
    }

    /**
     * assemble list result
     *
     * @param extractList
     * @return
     */
    private String assembleListResult(List extractList) {
        StringBuilder fieldValue = new StringBuilder("[");
        if (extractList.size() > 0) {
            fieldValue.append(assembleOneResultInList(extractList.get(0)));
            for (int i = 1; i < extractList.size(); i++) {
                fieldValue.append(",").append(assembleOneResultInList(extractList.get(i)));
            }
        }
        fieldValue.append("]");
        return fieldValue.toString();
    }

    /**
     * assemble object in list
     *
     * @param extractObj
     * @return
     */
    private Object assembleOneResultInList(Object extractObj) {
        return extractObj == null || extractObj instanceof Number ? extractObj
            : "\"" + assembleOneObject(extractObj) + "\"";
    }

    /**
     * build file descriptor with importing proto recursively
     *
     * @param currentFileProto current FileDescriptorProto
     * @param fileProtoCache   cache all <fileProtoName,FileDescriptorProto> pair
     * @return current file descriptor
     */
    private FileDescriptor buildFileDescriptor(FileDescriptorProto currentFileProto,
        Map<String, FileDescriptorProto> fileProtoCache) {
        List<FileDescriptor> dependencyFileDescriptorList = new ArrayList<>();
        currentFileProto.getDependencyList().forEach(dependencyStr -> {
            FileDescriptorProto dependencyFileProto = fileProtoCache.get(dependencyStr);
            FileDescriptor dependencyFileDescriptor = buildFileDescriptor(dependencyFileProto, fileProtoCache);
            dependencyFileDescriptorList.add(dependencyFileDescriptor);
        });
        try {
            return FileDescriptor.buildFrom(currentFileProto,
                dependencyFileDescriptorList.toArray(new FileDescriptor[0]));
        } catch (DescriptorValidationException e) {
            throw new IllegalStateException("FileDescriptor build fail!", e);
        }
    }

    public static final class ParserBuilder {
        private ParserBuilder() {}

        private String descFilePath;
        private String syntaxStr;

        public ParserBuilder descFilePath(String descFilePath) {
            this.descFilePath = descFilePath;
            return this;
        }

        public ParserBuilder syntax(String syntaxStr) {
            this.syntaxStr = syntaxStr;
            return this;
        }

        public DynamicPBParser build() throws IOException {
            DynamicPBParser parser = new DynamicPBParser();
            parser.load(descFilePath);
            if ("StandardSyntax".equals(syntaxStr)) {
                parser.syntax = StandardSyntax.create();
            } else {//there may be many syntax implements
                parser.syntax = StandardSyntax.create();
            }
            return parser;
        }
    }
}