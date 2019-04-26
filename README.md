  
# `dynamic-pb-parser` 介绍  
  
`dynamic-pb-parser`提供类似hive的`get_json_object`的功能，可以动态解析用Protobuf描述的数据。  
  
## 使用方法  
  
>因为Protobuf序列化的数据不能自解释，所以需要使用此函数的同学自行编译自己的proto文件为desc文件(命令见下文)，并将desc文件路径传入  
  
### 接入方式  
基于以上原因，此UDF提供两种接入方式  
1. 使用protoc命令将proto文件转换为desc文件(desc文件名可自定义)：  
    ```  
    protoc --include_imports -I. -otest.desc *.proto  
    ```  
2. 用法如下：  
   ```java  
    DynamicPBParser parser = DynamicPBParser.newBuilder()  
        .descFilePath("target/test-classes/test.desc")  
        .syntax("StandardSyntax")  
        .build();  
   parser.parse(content, 'me.lihongyu.bean.Person$name');  
   parser.parse(content, 'me.lihongyu.bean.Person$cloth.brand.type');  
   parser.parse(DynamicPBParser.parse(content, 'me.lihongyu.bean.Person$proto_data'), 'me.lihongyu.bean.AddressBook$email');  
   ```  
  
### 出参、入参和语法  
  
1. `DynamicPBParser.parse`有两个入参：  
   1. 用Base64编码后的pb数据  
   2. 需要解析的字段路径  
2. 字段路径语法：  
   1. 使用`$`符号分隔类名和字段名  
   2. 嵌套对象的格式：`package_name.message_name$field1_name.field2_name`  
  3. 嵌套数组的格式：`package_name.message_name$field1_name[*].field2_name[0]`，其中`field1_name[*]`也可简写为`field1_name`  
  4. 扩展字段  
       1. 对于message A 扩展字段x 定义在message B里的情况，解析x，可以把A的数据当作B来看，例：  
          
        ```  
        package a.b;  
        message A {  
            extensions 100 to 199;  
        }        
          
        package c.d;  
        message B {  
            extend A {  
                optional int32 x = 100;  
            }  
        }  
          
        data=Base64(A);  
        result = parser.parse(data, 'c.d.B$x');   
        ```  
          
       2. 对于message A 扩展字段x 未定义在某message里的情况，而是直接定义在package下的情况，解析x，需要写明确完整扩展字段路径，并用英文小括号`()`括起来，例：  
          
        ```  
        package a.b;  
        message A {  
            extensions 100 to 199;  
        }        
          
        package c.d;  
        extend A {  
            optional int32 x = 100;//protobuf认为此字段的完整路径是`c.d.x`  
        }  
          
        data=Base64(A);  
        result = parser.parse(data, "a.b.A$(c.d.x)");  
        ```   
3. 出参：  
	1. 永远是string类型
	2. 如果返回的是object
		1. 会返回用Base64编码后的object PB序列化结果，类似`"CggKBG5pa2UQARC2YA=="`
		2. 如果是Byte数组，会返回Base64编码后的字符串
		3. 其它情况返回toString()的结果
	3. 如果返回的是数组类型，会返回`[xxx,xxx,xxx]`
		1. 如果元素是数字类型，会返回数字本身，如`[1,2,3]`
		2. 其中当元素是object类型时，同2
  
## Q&A  
1. 发生异常`java.lang.IllegalArgumentException: XXX.XXX can not be found in any description file! Please check out if it exist.`，请检查desc文件中是否含有XXX.XXX的message定义，或仔细核对是否`package_name`和`message_name`有笔误  
2. 除问题1描述的场景外，其余错误统一返回null。如字段找不到、对象解析失败等  
  
## Todo List  
  
- [x] support extension field   
- [x] support array type  
- [x] exception or null?  
- [x] support proto importing    
- [x] optimize the performance