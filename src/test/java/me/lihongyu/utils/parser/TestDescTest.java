package me.lihongyu.utils.parser;

import com.google.protobuf.ByteString;
import me.lihongyu.utils.parser.test.AddressBookProtos;
import me.lihongyu.utils.parser.test.AddressBookProtos.AddressBook;
import me.lihongyu.utils.parser.test.AddressBookProtos.Person;
import me.lihongyu.utils.parser.test.AddressBookProtos.Person.Cloth;
import me.lihongyu.utils.parser.test.AddressBookProtos.Person.Cloth.Brand;
import me.lihongyu.utils.parser.test.AddressBookProtos.Person.Cloth.BrandType;
import me.lihongyu.utils.parser.test.AddressBookProtos.Person.PhoneNumber;
import me.lihongyu.utils.parser.test.AddressBookProtos.Person.PhoneType;
import me.lihongyu.utils.parser.test.AddressBookProtos.Person.Profession;
import me.lihongyu.utils.parser.test.ChildProtos;
import me.lihongyu.utils.parser.test.ChildProtos.Boy;
import me.lihongyu.utils.parser.test.ChildProtos.Child;
import me.lihongyu.utils.parser.test.NewProtos;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author jared
 * @date 2019/02/25
 */
public class TestDescTest {

    @Test(dependsOnMethods = "testSetup")
    public void testEvaluate() throws Exception {
        /*
            表达相同内容：
            'CghKb2huIERvZRDSCSIMCgg1NTUtNDMyMRABIgoKBjQzNjM0NhAAKg0KCAoEbmlrZRABELZg'
            '{"id":1234,"name":"John Doe","phones":[{"number":"555-4321","type":"HOME"},{"number":"436346",
            "type":"MOBILE"}]}'
         */
        PhoneNumber phoneNumber = PhoneNumber.newBuilder().setNumber("098754").setType(PhoneType.MOBILE).build();
        String base64Str = Base64.getEncoder().encodeToString(phoneNumber.toByteArray());
        System.out.println("PhoneNumber::::" + base64Str);
        DynamicPBParser parser = DynamicPBParser.newBuilder()
            .syntax("StandardSyntax")
            .descFilePath("target/test-classes/test.desc")
            .build();

        //测试空值
        assertThat(parser.parse(null, "biz.test.Person$cloth.price")).isNull();
        assertThat(parser.parse("", "biz.test.Person$cloth.price")).isNull();
        assertThat(parser.parse(base64Str, null)).isNull();
        assertThat(parser.parse(base64Str, "")).isNull();
        assertThat(parser.parse(null, "")).isNull();

        //测试不存在的schema
        assertThatIllegalArgumentException().isThrownBy(
                () -> parser.parse("CggKBG5pa2UQARC2YA==", "biz.test.Person_NonExist$non_exist"))
                .withMessageEndingWith(" can not be found in any description file! Please check out if it exist.");

        //测试不存在字段
        assertThat(parser.parse(base64Str, "biz.test.Person.PhoneNumber$non_exist")).isNull();

        //测试解析失败，返回null
        assertThat(parser.parse("TEST", "biz.test.Person.Cloth$price")).isNull();

        //测试内部类：phone number
        assertThat(parser.parse("CggKBG5pa2UQARC2YA==", "biz.test.Person.Cloth$price")).isEqualTo("12342");
        assertThat(parser.parse(base64Str, "biz.test.Person.PhoneNumber$number")).isEqualTo("098754");
        assertThat(parser.parse(base64Str, "biz.test.Person.PhoneNumber$type")).isEqualTo("MOBILE");

        Person john = assemblePerson();
        base64Str = Base64.getEncoder().encodeToString(john.toByteArray());
        System.out.println("Person::::" + base64Str);

        //测试连续字段
        assertThat(parser.parse(base64Str, "biz.test.Person$id")).isEqualTo("1234");
        assertThat(parser.parse(base64Str, "biz.test.Person$cloth")).isEqualTo("CggKBG5pa2UQARC2YA==");
        assertThat(parser.parse(base64Str, "biz.test.Person$cloth.price")).isEqualTo("12342");
        assertThat(parser.parse(base64Str, "biz.test.Person$cloth.price.something")).isNull();
        assertThat(parser.parse(base64Str, "biz.test.Person$cloth.brand.brand_name")).isEqualTo("nike");
        assertThat(parser.parse(base64Str, "biz.test.Person$cloth.brand.brand_type")).isEqualTo("SPORT");

        //测试数组
        //由对象组成的数组
        assertThat(parser.parse(base64Str, "biz.test.Person$phones[*]")).isEqualTo(
                "[\"Cgg1NTUtNDMyMQ==\",\"CgY0MzYzNDYQAA==\"]");
        assertThat(parser.parse(base64Str, "biz.test.Person$phones[0].number")).isEqualTo("555-4321");
        assertThat(parser.parse(base64Str, "biz.test.Person$phones[100].number")).isNull();
        assertThat(parser.parse(base64Str, "biz.test.Person$phones[*].number")).isEqualTo("[\"555-4321\",\"436346\"]");
        assertThat(parser.parse(base64Str, "biz.test.Person$phones[*].type")).isEqualTo("[\"HOME\",\"MOBILE\"]");
        assertThat(parser.parse(base64Str, "biz.test.Person$phones[*].type[1]")).isEqualTo("MOBILE");
        //由基本类型组成的数组
        assertThat(parser.parse(base64Str, "biz.test.Person$scores[*]")).isEqualTo("[100,120,150]");
        assertThat(parser.parse(base64Str, "biz.test.Person$scores[2]")).isEqualTo("150");
        assertThat(parser.parse(base64Str, "biz.test.Person$professions[*]")).isEqualTo("[\"PM\",\"PROGRAMMER\"]");
        assertThat(parser.parse(base64Str, "biz.test.Person$professions[0]")).isEqualTo("PM");

        //测试返回数组只有一个元素
        Person person2 = assemblePerson2();
        base64Str = Base64.getEncoder().encodeToString(person2.toByteArray());
        System.out.println("Person 2 ::::" + base64Str);
        assertThat(parser.parse(base64Str, "biz.test.Person$phones")).isEqualTo("[\"Cgg1NTUtNDMyMRAB\"]");

        //测试连续数组：addressbook
        AddressBook addressBook = assembleAddressBook();
        base64Str = Base64.getEncoder().encodeToString(addressBook.toByteArray());
        assertThat(parser.parse(base64Str, "biz.test.AddressBook$people[*].cloth.price")).isEqualTo("[12342,555]");
        assertThat(parser.parse(base64Str, "biz.test.AddressBook$people[*].cloth.price[1]")).isEqualTo("555");
        assertThat(parser.parse(base64Str, "biz.test.AddressBook$people[*].phones[*].type")).isEqualTo(
                "[\"HOME\",\"MOBILE\",\"MOBILE\",\"HOME\",\"MOBILE\"]");
        assertThat(parser.parse(base64Str, "biz.test.AddressBook$people.phones.type")).isEqualTo(
                "[\"HOME\",\"MOBILE\",\"MOBILE\",\"HOME\",\"MOBILE\"]");
        assertThat(parser.parse(base64Str, "biz.test.AddressBook$people.phones.type[4]")).isEqualTo("MOBILE");
        assertThat(parser.parse(base64Str, "biz.test.AddressBook$people[*].phones[0].type")).isEqualTo("HOME");
        assertThat(parser.parse(base64Str, "biz.test.AddressBook$people[1].phones[*].type")).isEqualTo(
                "[\"MOBILE\",\"HOME\",\"MOBILE\"]");
        assertThat(parser.parse(base64Str, "biz.test.AddressBook$people[1].phones[2].type")).isEqualTo("MOBILE");
        assertThat(parser.parse(base64Str, "biz.test.AddressBook$non_exist.phones[2].type")).isNull();

        //测试import的情况
        Child child = Child.newBuilder().setAddressBook(addressBook).setSchool("Tsinghua").setSex("male").build();
        base64Str = Base64.getEncoder().encodeToString(child.toByteArray());
        assertThat(parser.parse(base64Str, "biz.test.Child$school")).isEqualTo("Tsinghua");
        assertThat(parser.parse(base64Str, "biz.test.Child$address_book.people[0].name")).isEqualTo("John Doe");
        assertThat(parser.parse(base64Str, "biz.test.Child$address_book.people[*].phones[0].type")).isEqualTo("HOME");
    }

    @Test(dependsOnMethods = "testSetup")
    public void testExtensionFields() throws Exception {
        DynamicPBParser parser = DynamicPBParser.newBuilder()
            .syntax("StandardSyntax")
            .descFilePath("target/test-classes/test.desc")
            .build();

        byte[] bytes = {97, 98, 99, 100};
        AddressBook addressBook = assembleAddressBook();
        Child child = Child.newBuilder()
                .setAddressBook(addressBook)
                .setSchool("Tsinghua")
                .setSex("male")
                .setExtension(Boy.parent, "leon")
                .setExtension(Boy.scoreEnum, PhoneType.HOME)
                .setExtension(Boy.scoreInt, 456)
                .setExtension(Boy.scoreBool, Boolean.TRUE)
                .setExtension(Boy.scoreByte, ByteString.copyFrom(bytes))
                .setExtension(Boy.scoreDouble, 43.2)
                .setExtension(Boy.scoreLong, 55555555555L)
                .setExtension(Boy.scoreFloat, 14.5F)
                .setExtension(Boy.friend, assemblePerson())
                .setExtension(ChildProtos.age, 29)
                .setExtension(ChildProtos.university, "peking")
                .setExtension(ChildProtos.teacher, assemblePerson())
                .setExtension(NewProtos.girlFriend, assemblePerson2())
                .setExtension(ChildProtos.girlFriend, assemblePerson3())
                .build();

        String base64Str = Base64.getEncoder().encodeToString(child.toByteArray());
        assertThat(parser.parse(base64Str, "biz.test.Child$sex")).isEqualTo("male");
        assertThat(parser.parse(base64Str, "biz.test.Child$(biz.test.age)")).isEqualTo("29");
        assertThat(parser.parse(base64Str, "biz.test.Child$(biz.test.university)")).isEqualTo("peking");
        assertThat(parser.parse(base64Str, "biz.test.Child$(biz.test.teacher).professions[0]")).isEqualTo("PM");
        assertThat(parser.parse(base64Str, "biz.test.Boy$parent")).isEqualTo("leon");
        assertThat(parser.parse(base64Str, "biz.test.Boy$score_int")).isEqualTo("456");
        assertThat(parser.parse(base64Str, "biz.test.Boy$score_enum")).isEqualTo("HOME");
        assertThat(parser.parse(base64Str, "biz.test.Boy$score_bool")).isEqualTo("true");
        assertThat(parser.parse(base64Str, "biz.test.Boy$score_byte")).isEqualTo(
                Base64.getEncoder().encodeToString("abcd".getBytes()));
        assertThat(parser.parse(base64Str, "biz.test.Boy$score_long")).isEqualTo("55555555555");
        assertThat(parser.parse(base64Str, "biz.test.Boy$friend.cloth.brand.brand_name")).isEqualTo("nike");
        assertThat(parser.parse(base64Str, "biz.test.Boy$score_float")).isEqualTo("14.5");
        assertThat(parser.parse(base64Str, "biz.test.Boy$score_double")).isEqualTo("43.2");
        assertThat(parser.parse(base64Str, "biz.test.Child$(biz.test.new.girlFriend).id")).isEqualTo("1234");
        assertThat(parser.parse(base64Str, "biz.test.Child$(biz.test.girlFriend).id")).isEqualTo("7777");
        assertThat(parser.parse(parser.parse(base64Str, "biz.test.Child$(biz.test.girlFriend)"), "biz.test.Person$(biz.test.feet)")).isEqualTo("44");
        //以下这种连续扩展字段的做法目前还不支持，可以用上面这种连续使用udf的方式
        assertThat(parser.parse(base64Str, "biz.test.Child$(biz.test.girlFriend).(biz.test.feet)")).isEqualTo("44");
        assertThat(parser.parse(base64Str, "biz.test.Child$address_book.people[*].(biz.test.feet)")).isEqualTo("[33]");
        assertThat(parser.parse(base64Str, "biz.test.Child$address_book.people[*].(biz.test.feet)[0]")).isEqualTo("33");
        assertThat(parser.parse(base64Str, "biz.test.Child$address_book.people[*].(biz.test.feet)[1]")).isNull();
        assertThat(parser.parse(base64Str, "biz.test.Child$address_book.people[1].(biz.test.feet)")).isEqualTo("33");
    }

    private AddressBook assembleAddressBook() {
        return AddressBook.newBuilder()
                .addPeople(assemblePerson())
                .addPeople(
                        Person.newBuilder()
                                .setId(5432)
                                .setName("leon")
                                .setCloth(
                                        Cloth.newBuilder()
                                                .setPrice(555)
                                                .setBrand(
                                                        Brand.newBuilder()
                                                                .setBrandType(BrandType.CASUAL)
                                                                .setBrandName("free solo")
                                                                .build()
                                                ).build())
                                .addPhones(PhoneNumber.newBuilder().setType(PhoneType.MOBILE).setNumber("456456456").build())
                                .addPhones(PhoneNumber.newBuilder().setType(PhoneType.HOME).setNumber("123123123").build())
                                .addPhones(PhoneNumber.newBuilder().setType(PhoneType.MOBILE).setNumber("999888").build())
                                .setExtension(AddressBookProtos.feet, 33)
                )
                .build();
    }

    private Person assemblePerson() {
        return Person.newBuilder()
                .setId(1234)
                .setName("John Doe")
                //.setEmail("jdoe@example.com")
                .addPhones(
                        PhoneNumber.newBuilder()
                                .setNumber("555-4321")
                        //.setType(PhoneType.HOME)
                )
                .addPhones(
                        PhoneNumber.newBuilder()
                                .setNumber("436346")
                                .setType(PhoneType.MOBILE)
                )
                .addScores(100)
                .addScores(120)
                .addScores(150)
                .addProfessions(Profession.PM)
                .addProfessions(Profession.PROGRAMMER)
                .setCloth(
                        Cloth.newBuilder()
                                .setBrand(
                                        Cloth.Brand.newBuilder()
                                                .setBrandName("nike")
                                                .setBrandType(Cloth.BrandType.SPORT)
                                                .build()
                                )
                                .setPrice(12342)
                                .build()
                )
                //.setExtension(AddressBookProtos.feet, 32)
                .build();
    }

    private Person assemblePerson2() {
        return Person.newBuilder()
                .setId(1234)
                .setName("John Doe")
                //.setEmail("jdoe@example.com")
                .addPhones(
                        PhoneNumber.newBuilder()
                                .setNumber("555-4321")
                                .setType(PhoneType.HOME)
                )
                .setCloth(
                        Cloth.newBuilder()
                                .setBrand(
                                        Cloth.Brand.newBuilder()
                                                .setBrandName("nike")
                                                .setBrandType(Cloth.BrandType.SPORT)
                                                .build()
                                )
                                .setPrice(12342)
                                .build()
                )
                .build();
    }

    private Person assemblePerson3() {
        return Person.newBuilder()
                .setId(7777)
                .setName("Mike")
                //.setEmail("jdoe@example.com")
                .addPhones(
                        PhoneNumber.newBuilder()
                                .setNumber("4444222")
                                .setType(PhoneType.HOME)
                )
                .setCloth(
                        Cloth.newBuilder()
                                .setBrand(
                                        Cloth.Brand.newBuilder()
                                                .setBrandName("nike")
                                                .setBrandType(Cloth.BrandType.SPORT)
                                                .build()
                                )
                                .setPrice(12342)
                                .build()
                )
                .setExtension(AddressBookProtos.feet, 44)
                .build();
    }

    @Test
    public void testSetup() throws IOException {
        //nothing
    }

}

