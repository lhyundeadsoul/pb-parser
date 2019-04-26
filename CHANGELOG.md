
## [1.2.2] - 2019-03-26
### Added
- 支持多个`()`代表的扩展字段，e.g. `biz.test.Child$(biz.test.girlFriend).(biz.test.feet)`
- 支持`()`代表的扩展字段数组，e.g. `biz.test.Child$address_book.people[*].(biz.test.feet)[1]`

### Changed
- 返回数组时，其中不允许有 null 元素

## [1.2.1] - 2019-03-25
### Fixed
- 修复`return "null"`的问题

## [1.2.0] - 2019-03-25
### Added
-  增加了`extension_field`的语法，用`()`代表package下定义的扩展字段，完整支持扩展字段

## [1.1.3] - 2019-03-13
### Changed
- odps endpoint 从办公网endpoint换成生产环境，避免个别情况下会出现的Connection Refused Exception

## [1.1.2] - 2019-03-12
### Changed
- descriptorCache 从List改为Map，提升查询效率

## [1.1.1] - 2019-03-08
### Fixed
- 修复proto文件互相import时，build FileDescriptor的异常，详见Issue #68337
- 修复bytes类型返回错误，改为返回Base64编码后的字符串

## [1.1.0] - 2019-03-07
### Added
- 支持通过odps sdk得到desc文件，避免每个用户都自己创建UDF

## [1.0.0] - 2019-03-07
### Added
- 支持嵌套对象
- 支持嵌套数组

