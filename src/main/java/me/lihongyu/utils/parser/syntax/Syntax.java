package me.lihongyu.utils.parser.syntax;

/**
 * field path syntax
 * @author jared
 */
public abstract class Syntax {

    /**
     * check whether the fullPathStr conforms to this syntax
     * @param fullPathStr
     * @return
     */
    public abstract boolean check(String fullPathStr);

    /**
     * get message full name string
     * @param fullPathStr
     * @return
     */
    public abstract String getClassFullName(String fullPathStr);

    /**
     * get the whole field path string
     * @param fullPathStr
     * @return
     */
    public abstract String getFieldPathStr(String fullPathStr);
    /**
     * get wildcard char
     * @return
     */
    public abstract String getWildcard();
    /**
     * extract name of field
     *
     * @param fieldPath
     * @return
     */
    public abstract String getFieldName(String fieldPath);

    /**
     * extract index of field
     *
     * @param fieldPath
     * @return
     */
    public abstract String getFieldIndex(String fieldPath);

    /**
     * split and cache field path
     *
     * @param fieldPathStr
     * @return
     */
    public abstract String[] getFieldPathArr(String fieldPathStr);
}
