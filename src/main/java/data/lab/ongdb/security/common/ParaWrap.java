package data.lab.ongdb.security.common;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import java.util.Collection;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.common
 * @Description: TODO
 * @date 2022/2/24 16:50
 */
public class ParaWrap {

    public static String withParamMapping(String fragment, Collection<String> keys) {
        if (keys.isEmpty()) {
            return fragment;
        }
        String declaration = " WITH " + keys.stream().map(s -> format(" {`%s`} as `%s` ", s, s)).collect(Collectors.joining(", "));
        return declaration + fragment;
    }

}

