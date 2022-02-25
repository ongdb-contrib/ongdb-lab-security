package data.lab.ongdb.security.common;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.common
 * @Description: TODO
 * @date 2022/2/24 16:50
 */
public class ParaWrap {

    /**
     * 包装GQL查询参数
     * **/
    public static String withParamMapping(String fragment, Collection<String> keys) {
        if (keys.isEmpty()) {
            return fragment;
        }
        String declaration = " WITH " + keys.stream().map(s -> format(" {`%s`} as `%s` ", s, s)).collect(Collectors.joining(", "));
        return declaration + fragment;
    }

    /**
     * 列表排重
     * **/
    public static <T> Predicate<T> distinctById(Function<? super T, ?> idExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(idExtractor.apply(t), Boolean.TRUE) == null;
    }

}

