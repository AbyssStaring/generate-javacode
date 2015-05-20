package com.jd.code.generate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jd.code.generate.domain.Field;
import com.jd.code.generate.util.Utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

import static com.jd.code.generate.util.Utils.readProperties;

/**
 * User: yangkuan@jd.com
 * Date: 15-1-13
 * Time: 下午4:34
 */
public class VelocityParaContext {

      Map<String, Object> commonContext = new HashMap<String, Object>();


    static  Map<String, Field> fieldsMap = new HashMap<String, Field>();//key是表字段名字

    static Map<String,String> classNameMap = new HashMap<String,String>();//key是表名,value表示类名
    static Map<String,String> instNameMap = new HashMap<String,String>();//key是表名,value表示类的变量名

    static Map<String,String> camelFieldNameMap = new HashMap<String,String>();//key是表的字段名,value表示类的属性（用驼峰表示）

    static {
        readProperties("DB2ClassMapping/classNameMapping.properties", classNameMap);
        readProperties("DB2ClassMapping/classVarNameMapping.properties", instNameMap);
        readProperties("DB2ClassMapping/ClassFieldNameMapping.properties", camelFieldNameMap);
    }

    /*
	 * 生成velocity模版里的参数
	 */
    public  Map<String, Object> generateTable2VelocityParameter(String sqlserverType, Connection conn, String tableName) throws SQLException {
        commonContext.clear();


        Set<String> imports = new HashSet<String>();
        List<Field> fields = new ArrayList<Field>();

        commonContext.put(Constant.CLASS_NAME, classNameMap.get(tableName)==null?tableName:classNameMap.get(tableName));
        commonContext.put(Constant.TABLE_NAME, tableName);
        commonContext.put(Constant.INST_NAME, instNameMap.get(tableName)==null?tableName:instNameMap.get(tableName));
        // commonContext.put(PK_ID, pkId);
        String pkField = null;
        List<String> commentList = null;
        String sql = null;
        if (sqlserverType.equals("mssql")) {
            pkField = getMSSQLPkField(conn, tableName);
            sql = "select top 1 * from [" + tableName + "]";
        } else if (sqlserverType.equals("mysql")) {
            pkField = getMYSQLPkField(conn, tableName);

            commentList = getMysqlCommentList(conn, tableName);
            sql = "select * from " + tableName + " limit 1";
        }
        commonContext.put(Constant.PK_ID, pkField);
        commonContext.put(Constant.PK_NAME, pkField.substring(0, 1).toLowerCase() + pkField.substring(1));
        String pkType = null;
        String pkJavaFullType = null;
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        ResultSetMetaData rsmd = rs.getMetaData();
        // 表的字段数量
        int columnCounts = rsmd.getColumnCount();
        for (int i = 1; i <= columnCounts; i++) {
            String name = rsmd.getColumnName(i);
            String columnClassName = rsmd.getColumnClassName(i);
            String fdname = name;
            String javaType = columnClassName.substring(columnClassName.lastIndexOf(".") + 1);
            boolean isPk = (pkField != null && pkField.equalsIgnoreCase(fdname)) || (pkField == null && i == 1);
            if (isPk) {
                pkType = javaType;
                pkJavaFullType = columnClassName;
            }

            Field field = new Field();

            field.setFieldName(name);
            System.out.println(name);
            String fieldName = camelFieldName(name);
            field.setPropertyName(fieldName);
            field.setSetterName("set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
            field.setGetterName("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
            field.setJavaType(javaType);
            field.setJdbcType(rsmd.getColumnTypeName(i));
            field.setJavaFullType(rsmd.getColumnClassName(i));
            field.setComment(commentList.get(i - 1));
            fields.add(field);
            fieldsMap.put(name,field);
            if (field.getJavaFullType().indexOf("java.lang") == -1) {
                imports.add(field.getJavaFullType());
            }
        }
        getMYSQLIndexField(conn, tableName);
        commonContext.put(Constant.FIELDS, fields);
        commonContext.put(Constant.PK_TYPE, pkType);
        commonContext.put(Constant.IMPORTS, imports);
        commonContext.put(Constant.AUTHOR, "yangkuan@jd.com");
        commonContext.put("result", "result");

        commonContext.put(Constant.pk_JavaFullType, pkJavaFullType);
        return commonContext;
    }



    private static String camelFieldName(String fieldName){
        return camelFieldNameMap.get(fieldName)==null?fieldName:camelFieldNameMap.get(fieldName);
    }

    /*
     * 得到MSSQL表的主键id
     */
    public static String getMSSQLPkField(Connection conn, String tableName) throws SQLException {
        PreparedStatement pstmt;
        String pkField = null;
        pstmt = conn
                .prepareStatement("SELECT syscolumns.name From sysobjects inner join syscolumns on sysobjects.id = syscolumns.id left outer join (select  o.name sTableName, c.Name sColName From  sysobjects o  inner join sysindexes i on o.id = i.id and (i.status & 0X800) = 0X800 inner join syscolumns c1 on c1.colid <= i.keycnt and c1.id = o.id inner join syscolumns c on o.id = c.id and c.name = index_col (o.name, i.indid, c1.colid)) pkElements on pkElements.sTableName = sysobjects.name and pkElements.sColName = syscolumns.name inner join sysobjects syscons on sysobjects.id=syscons.parent_obj and syscons.xtype='PK' where (syscolumns.Status & 128)=128 and sysobjects.name=?");
        pstmt.setString(1, tableName);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            pkField = rs.getString(1);
        }
        pstmt.close();
        return pkField;
    }

    /*
     * 得到MYSQL表的主键id
     */
    public static String getMYSQLPkField(Connection conn, String tableName) throws SQLException {
        PreparedStatement pstmt;
        String pkField = null;
        pstmt = conn.prepareStatement("show index from " + tableName + "  where Key_name='PRIMARY'");
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            pkField = rs.getString(5);
            if (pkField.indexOf(",") > 0) {
                pkField = pkField.substring(0, pkField.indexOf(","));

            }
            //System.out.println("MYSQL表" + tableName + "主键id=" + pkField);

        }
        pstmt.close();
        return pkField;
    }

    /*
 * 得到MYSQL表的index
 */
    public   String getMYSQLIndexField(Connection conn, String tableName) throws SQLException {
        PreparedStatement pstmt;
        String pkField = null;
        pstmt = conn.prepareStatement("show index from " + tableName );
        ResultSet rs = pstmt.executeQuery();

        Multimap<String, Field> uniqueIndexMultiMap = ArrayListMultimap.create();
        Multimap<String, Field> commonIndexMultiMap = ArrayListMultimap.create();
        while (rs.next()) {
            String nonUnique = rs.getString(2);
            String keyName = rs.getString(3);
            String columnName = rs.getString(5);
            if(keyName.equals("PRIMARY")){//主键
                commonContext.put(Constant.PK_ID, columnName);
            }else{//普通索引和唯一索引
                if(nonUnique.equals("0")){//唯一索引
                    uniqueIndexMultiMap.put(keyName,fieldsMap.get(columnName));
                }
                if(nonUnique.equals("1")){//普通索引
                    commonIndexMultiMap.put(keyName,fieldsMap.get(columnName));
                }
            }
        }
        commonContext.put(Constant.Unique_index,uniqueIndexMultiMap);
        commonContext.put(Constant.Common_index,commonIndexMultiMap);
        List<Field> fields = (List<Field>) commonIndexMultiMap.get("merchantId_index");
        pstmt.close();
        return pkField;
    }

    /*
     * 得到mysql表列注释集合
     */
    public static List<String> getMysqlCommentList(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData dmd = conn.getMetaData();
        ResultSet rs = dmd.getColumns(null, null, tableName, null);
        List<String> commentList = new ArrayList<String>();
        int ix = 1;
        while (rs.next()) {
            //System.out.println(rs.getString("REMARKS"));
            commentList.add(rs.getString("REMARKS"));
            ix += 1;
        }
        return commentList;
    }

    private static class Constant{
       static   String CLASS_NAME = "className";
        static  String TABLE_NAME = "tableName";
        static  String INST_NAME = "instName";
        static String IMPORTS = "imports";
        static String AUTHOR = "author";
        static String PK_ID = "pkid";
        // javabean 主键java类型
        static String PK_TYPE = "pkType";
        // javabean 主键的java全路径类�?
        static String pk_JavaFullType = "pkJavaFullType";
        static  String PK_NAME = "pkname";
        static  String FIELDS = "fields";

        static  String Unique_index = "uniqueIndex";
        static  String Common_index = "commonIndex";
    }
}
