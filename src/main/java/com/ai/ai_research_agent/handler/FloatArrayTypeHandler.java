package com.ai.ai_research_agent.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * float[] 数组类型处理器，用于映射 PostgreSQL 的 vector 类型
 */
public class FloatArrayTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        Array array = ps.getConnection().createArrayOf("float8", convertToObjectArray(parameter));
        ps.setArray(i, array);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array array = rs.getArray(columnName);
        return convertToFloatArray(array);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array array = rs.getArray(columnIndex);
        return convertToFloatArray(array);
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array array = cs.getArray(columnIndex);
        return convertToFloatArray(array);
    }

    private Object[] convertToObjectArray(float[] floatArray) {
        if (floatArray == null) {
            return null;
        }
        Object[] result = new Object[floatArray.length];
        for (int i = 0; i < floatArray.length; i++) {
            result[i] = floatArray[i];
        }
        return result;
    }

    private float[] convertToFloatArray(Array array) throws SQLException {
        if (array == null) {
            return null;
        }
        Object[] objectArray = (Object[]) array.getArray();
        float[] floatArray = new float[objectArray.length];
        for (int i = 0; i < objectArray.length; i++) {
            floatArray[i] = ((Number) objectArray[i]).floatValue();
        }
        return floatArray;
    }
}
