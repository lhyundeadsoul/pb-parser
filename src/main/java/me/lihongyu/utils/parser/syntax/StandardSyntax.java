package me.lihongyu.utils.parser.syntax;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * standard syntax : pkg_name.message_name$field1.field2
 * @author jared
 */
public class StandardSyntax extends Syntax {
    private StandardSyntax() {
    }

    public static StandardSyntax create() {
        return new StandardSyntax();
    }

    /**
     * pattern of field name
     */
    private static final Pattern PATTERN_KEY = Pattern.compile("^([a-zA-Z0-9_.\\-\\:\\s]+).*");
    /**
     * pattern of field index
     */
    private static final Pattern PATTERN_INDEX = Pattern.compile("\\[([0-9]+|\\*)\\]");
    private static final char CLASS_SEPARATOR = '$';
    private static final char FIELD_SEPARATOR = '.';
    private static final char EXTENSION_FIELD_PREFIX = '(';
    private static final char EXTENSION_FIELD_SUFFIX = ')';
    private static final char ARRAY_FIELD_PREFIX = '[';
    private static final char ARRAY_FIELD_SUFFIX = ']';
    private static final String WILDCARD = "*";
    /**
     * fullPathArr[0] is class name
     * fullPathArr[1] is field name
     */
    private String[] fullPathArr;
    /**
     * cache: field path -> the Name part of the field
     */
    private final Map<String, String> fieldNameCache = Maps.newHashMapWithExpectedSize(16);
    /**
     * cache: field path -> the Index part of the field
     */
    private final Map<String, String> fieldIndexCache = Maps.newHashMapWithExpectedSize(16);
    /**
     * cache: fullFieldPath -> path array
     * e.g. friend.cloth.brand.brand_name -> [friend, cloth, brand, brand_name]
     */
    private final Map<String, String[]> fieldPathCache = Maps.newHashMapWithExpectedSize(16);

    @Override
    public String getClassFullName(String fullPathStr) {
        return fullPathArr[0];
    }

    @Override
    public String getFieldPathStr(String fullPathStr) {
        return fullPathArr[1];
    }

    @Override
    public String getWildcard() {
        return WILDCARD;
    }

    @Override
    public String getFieldName(String fieldPath) {
        if (!fieldNameCache.containsKey(fieldPath)) {
            Matcher matcher = PATTERN_KEY.matcher(fieldPath);
            fieldNameCache.put(fieldPath, matcher.find() ? matcher.group(1) : null);
        }
        return fieldNameCache.get(fieldPath);
    }

    @Override
    public String getFieldIndex(String fieldPath) {
        if (!fieldIndexCache.containsKey(fieldPath)) {
            Matcher matcher = PATTERN_INDEX.matcher(fieldPath);
            fieldIndexCache.put(fieldPath, matcher.find() ? matcher.group(1) : null);
        }
        return fieldIndexCache.get(fieldPath);
    }


    @Override
    public String[] getFieldPathArr(String fieldPathStr) {
        if (!fieldPathCache.containsKey(fieldPathStr)) {
            List<String> fieldPathList = new ArrayList<>();
            String tmpPath = fieldPathStr;
            //regard xxx in `(xxx)` as a whole field
            while (tmpPath.indexOf(EXTENSION_FIELD_PREFIX) != -1 && tmpPath.indexOf(EXTENSION_FIELD_SUFFIX) != -1) {
                //put all fields in front of `() syntax` in field path list
                String[] split = StringUtils.split(tmpPath.substring(0, tmpPath.indexOf(EXTENSION_FIELD_PREFIX)), '.');
                Collections.addAll(fieldPathList, split);
                //get extensionFieldName and put it in field path list
                String extensionFieldName = tmpPath.substring(tmpPath.indexOf(EXTENSION_FIELD_PREFIX) + 1, tmpPath.indexOf(EXTENSION_FIELD_SUFFIX));
                int endIndex = tmpPath.indexOf(EXTENSION_FIELD_SUFFIX);
                if (tmpPath.indexOf(EXTENSION_FIELD_SUFFIX) + 1 < tmpPath.length()
                        && tmpPath.charAt(tmpPath.indexOf(EXTENSION_FIELD_SUFFIX) + 1) == ARRAY_FIELD_PREFIX) {
                    String extensionFieldIndex = tmpPath.substring(tmpPath.indexOf(EXTENSION_FIELD_SUFFIX) + 1, tmpPath.indexOf(ARRAY_FIELD_SUFFIX, tmpPath.indexOf(EXTENSION_FIELD_SUFFIX) + 1) + 1);
                    extensionFieldName += extensionFieldIndex;
                    endIndex = tmpPath.indexOf(ARRAY_FIELD_SUFFIX, tmpPath.indexOf(EXTENSION_FIELD_SUFFIX) + 1);
                }
                fieldPathList.add(extensionFieldName);

                //for next loop
                tmpPath = tmpPath.substring(endIndex + 1);
            }
            //put last path in field path list
            String[] split = StringUtils.split(tmpPath, FIELD_SEPARATOR);
            Collections.addAll(fieldPathList, split);

            //cache it
            fieldPathCache.put(fieldPathStr, fieldPathList.toArray(new String[0]));
        }
        return fieldPathCache.get(fieldPathStr);
    }

    @Override
    public boolean check(String fullPathStr) {
        fullPathArr = StringUtils.split(fullPathStr, CLASS_SEPARATOR);
        return StringUtils.isNotBlank(fullPathStr) && ArrayUtils.isNotEmpty(fullPathArr) && fullPathArr.length == 2;
    }
}
