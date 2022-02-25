package data.lab.ongdb.security.util;

/*
 *
 * Data Lab - graph database organization.
 *
 */

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import data.lab.ongdb.security.role.Publisher;
import data.lab.ongdb.security.role.Reader;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.util.FileUtil
 * @Description: TODO
 * @date 2022/2/22 10:04
 */
public class FileUtil {

    public static final String ENCODING = "UTF-8";

    public static final String PATH_DIR = "auth";

    /**
     * @param
     * @return
     * @Description: TODO(Read all line)
     */
    public static String readFile(String filePath, String encoding) {
        File file = new File(filePath);
        if (file.exists()) {
            long fileLength = file.length();
            byte[] fileContent = new byte[(int) fileLength];

            FileInputStream in = null;
            try {
                in = new FileInputStream(file);
                in.read(fileContent);
                return new String(fileContent, encoding);

            } catch (UnsupportedEncodingException e) {
                System.err.println("The OS does not support " + encoding);
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param content:写入内容
     * @param filename:文件名
     * @param append:是否追加
     * @return
     * @Description: TODO
     */
    public static void writeToFile(String content, String filename, boolean append) {
        try {
            File dir = new File(PATH_DIR);
            if (!dir.exists()) {
                boolean bool = dir.mkdirs();
                System.out.println("mkdir:" + PATH_DIR + ",status:" + bool);
            }
            File file = new File(dir, filename);
            FileWriter writer = new FileWriter(file, append);
            writer.write(content + "\r\n");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param fileName:配置权限验证文件路径
     * @param username:用户名
     * @param encoding:编码格式
     * @return
     * @Description: TODO
     */
    public static Publisher readPublisherAuth(String fileName, String username, String encoding) {
        String string = readFile(PATH_DIR + File.separator + fileName, encoding);
        if (Objects.nonNull(string) && !"".equals(string)) {
            JSONArray array = JSONArray.parseArray(string)
                    .parallelStream()
                    .filter(v -> {
                        JSONObject object = (JSONObject) v;
                        return username.equals(object.getString("username"));
                    })
                    .collect(Collectors.toCollection(JSONArray::new));
            if (!array.isEmpty()) {
                JSONObject pubObj = array.getJSONObject(0);
                return new Publisher(
                        pubObj.getString("username"),
                        pubObj.getString("currentRole"),
                        convertPublisherNodeLabels(pubObj.getJSONArray("nodeLabels")),
                        convertPublisherRelTypes(pubObj.getJSONArray("relTypes"))
                );
            }
        }
        return null;
    }

    private static List<Map<String, Object>> convertPublisherRelTypes(JSONArray publisherRelTypes) {
        if (Objects.nonNull(publisherRelTypes)) {
            return publisherRelTypes.parallelStream()
                    .map(v -> {
                        JSONObject object = (JSONObject) v;
                        return new HashMap<String, Object>() {{
                            put("start_label", object.getString("start_label"));
                            put("type", object.getString("type"));
                            put("end_label", object.getString("end_label"));
                            put("properties", convertProsListMap(object.getJSONArray("properties")));
                            put("operator", object.getString("operator"));
                            put("invalid_values", object.getJSONArray("invalid_values"));
                        }};
                    })
                    .collect(Collectors.toList());
        }
        return null;
    }

    private static List<Map<String, Object>> convertProsListMap(JSONArray properties) {
        if (Objects.nonNull(properties)) {
            return properties
                    .parallelStream()
                    .map(v -> {
                        JSONObject object = (JSONObject) v;
                        return new HashMap<String, Object>() {{
                            put("field", object.getString("field"));
                            put("operator", object.getString("operator"));
                            put("check", object.getString("check"));
                            put("invalid_values", object.getJSONArray("invalid_values"));
                        }};
                    })
                    .collect(Collectors.toList());
        }
        return null;
    }

    private static List<Map<String, Object>> convertPublisherNodeLabels(JSONArray publisherNodeLabels) {
        if (Objects.nonNull(publisherNodeLabels)) {
            return publisherNodeLabels
                    .parallelStream()
                    .map(v -> {
                        JSONObject object = (JSONObject) v;
                        return new HashMap<String, Object>() {{
                            put("label", object.getString("label"));
                            put("properties", convertProsListMap(object.getJSONArray("properties")));
                            put("operator", object.getString("operator"));
                            put("invalid_values", object.getJSONArray("invalid_values"));
                        }};
                    })
                    .collect(Collectors.toList());
        }
        return null;
    }

    /**
     * @param fileName:权限配置文件名
     * @param publisher:合并之后用户权限
     * @return
     * @Description: TODO
     */
    public static void writePublisherAuth(String fileName, Publisher publisher) {
        String string = readFile(PATH_DIR + File.separator + fileName, ENCODING);
        JSONArray result;
        JSONObject object = JSONObject.parseObject(JSONObject.toJSONString(publisher));
        if (Objects.nonNull(string) && !"".equals(string)) {
            JSONArray array = JSONArray.parseArray(string);

            JSONArray arrayFilter = array.parallelStream()
                    .filter(v -> {
                        JSONObject obj = (JSONObject) v;
                        return !publisher.username.equals(obj.get("username"));
                    })
                    .collect(Collectors.toCollection(JSONArray::new));
            arrayFilter.add(object);
            result = arrayFilter;

        } else {
            result = new JSONArray() {
                {
                    add(object);
                }
            };
        }
        writeToFile(result.toJSONString(), fileName, false);
    }

    /**
     * @param fileName:配置权限验证文件路径
     * @param username:用户名
     * @param encoding:编码格式
     * @return
     * @Description: TODO
     */
    public static Reader readReaderAuth(String fileName, String username, String encoding) {
        String string = readFile(PATH_DIR + File.separator + fileName, encoding);
        if (Objects.nonNull(string) && !"".equals(string)) {
            JSONArray array = JSONArray.parseArray(string)
                    .parallelStream()
                    .filter(v -> {
                        JSONObject object = (JSONObject) v;
                        return username.equals(object.getString("username"));
                    })
                    .collect(Collectors.toCollection(JSONArray::new));
            if (!array.isEmpty()) {
                JSONObject pubObj = array.getJSONObject(0);
                return new Reader(
                        pubObj.getString("username"),
                        pubObj.getString("currentRole"),
                        convertReaderQueries(pubObj.getJSONArray("queries"))
                );
            }
        }
        return null;
    }

    private static List<Map<String, String>> convertReaderQueries(JSONArray queries) {
        if (Objects.nonNull(queries)) {
            return queries
                    .parallelStream()
                    .map(v -> {
                        JSONObject object = (JSONObject) v;
                        return new HashMap<String, String>() {{
                            put("query_id", object.getString("query_id"));
                            put("query", object.getString("query"));
                        }};
                    })
                    .collect(Collectors.toList());
        }
        return null;
    }

    /**
     * @param fileName:权限配置文件名
     * @param reader:合并之后用户权限
     * @return
     * @Description: TODO
     */
    public static void writeReaderAuth(String fileName, Reader reader) {
        String string = readFile(PATH_DIR + File.separator + fileName, ENCODING);
        JSONArray result;
        JSONObject object = JSONObject.parseObject(JSONObject.toJSONString(reader));
        if (Objects.nonNull(string) && !"".equals(string)) {
            JSONArray array = JSONArray.parseArray(string);

            JSONArray arrayFilter = array.parallelStream()
                    .filter(v -> {
                        JSONObject obj = (JSONObject) v;
                        return !reader.username.equals(obj.get("username"));
                    })
                    .collect(Collectors.toCollection(JSONArray::new));
            arrayFilter.add(object);
            result = arrayFilter;

        } else {
            result = new JSONArray() {
                {
                    add(object);
                }
            };
        }
        writeToFile(result.toJSONString(), fileName, false);
    }

    /**
     * @param filename:文件名
     * @param username:用户名
     * @param encoding:编码格式
     * @return
     * @Description: TODO
     */
    public static void clearAuth(String filename, String username, String encoding) {
        String string = readFile(PATH_DIR + File.separator + filename, encoding);
        if (Objects.nonNull(string) && !"".equals(string)) {
            JSONArray array = JSONArray.parseArray(string);

            JSONArray arrayFilter = array.parallelStream()
                    .filter(v -> {
                        JSONObject obj = (JSONObject) v;
                        return !username.equals(obj.get("username"));
                    })
                    .collect(Collectors.toCollection(JSONArray::new));
            writeToFile(arrayFilter.toJSONString(), filename, false);
        }
    }

    /**
     * 读取权限文件内容，合并到一个列表之后返回
     *
     * @param filename:权限文件名
     * @return
     * @Description: TODO
     */
    public static List<Object> readAuthList(String... filename) {
        List<Object> list = new ArrayList<>();
        for (String file : filename) {
            String string = FileUtil.readFile(PATH_DIR + File.separator + file, FileUtil.ENCODING);
            if (Objects.nonNull(string) && !"".equals(string)) {
                JSONArray array = JSONArray.parseArray(string);
                list.addAll(array.parallelStream()
                        .collect(Collectors.toList()));
            }
        }
        return list;
    }
}



