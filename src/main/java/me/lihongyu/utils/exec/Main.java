package me.lihongyu.utils.exec;

import java.io.IOException;

import me.lihongyu.utils.parser.DynamicPBParser;

/**
 * main entrance
 * @author jared
 * @date 2019/04/17
 */
public class Main {
    public static void main(String[] args) throws IOException {
        DynamicPBParser parser = DynamicPBParser.newBuilder()
            .descFilePath(args[0])
            .syntax(args[1])
            .build();
        System.out.println(parser.parse(args[2], args[3]));
    }
}
