// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: new.proto

package me.lihongyu.utils.parser.test;

public final class NewProtos {
  private NewProtos() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
    registry.add(NewProtos.girlFriend);
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public static final int GIRLFRIEND_FIELD_NUMBER = 170;
  /**
   * <code>extend .biz.test.Child { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      ChildProtos.Child,
      AddressBookProtos.Person> girlFriend = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        AddressBookProtos.Person.class,
        AddressBookProtos.Person.getDefaultInstance());

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\tnew.proto\022\014biz.test.new\032\013child.proto\032\021" +
      "addressbook.proto:6\n\ngirlFriend\022\017.biz.te" +
      "st.Child\030\252\001 \001(\0132\020.biz.test.PersonB&\n\031com" +
      ".autonavi.snowman.testB\tNewProtos"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          ChildProtos.getDescriptor(),
          AddressBookProtos.getDescriptor(),
        }, assigner);
    girlFriend.internalInit(descriptor.getExtensions().get(0));
    ChildProtos.getDescriptor();
    AddressBookProtos.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
