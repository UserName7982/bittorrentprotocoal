package EncodingFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class endcode {

    // String encode <length>:<string>
    // integer encode i:<integer>
    // List encode l:<item1>...<itemN>e
    // Map<String,Object> encode d:<key1>:<value1>...<keyN>:<valueN>e
    // byte[] encode <length>:<data>

    @SuppressWarnings("unchecked")
    public Object encode(Object data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (data == null) {
            return "";
        }

        else if (data instanceof String) {
            return stringencoder((String) data, out);
        } else if (data instanceof Integer) {
            return integerencoder((Integer) data, out);
        } else if (data instanceof byte[]) {
            return bytearrayencoder((byte[]) data, out);
        } else if (data instanceof List) {
            return listencoder((List<Object>) data, out);
        } else if (data instanceof Map) {
            return mapencoder((Map<String, Object>) data, out);
        }
        return null;
    }

    private Object mapencoder(Map<String, Object> data, ByteArrayOutputStream out) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mapencoder'");
    }

    private Object listencoder(List<Object> data, ByteArrayOutputStream out) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listencoder'");
    }

    private Object bytearrayencoder(byte[] data, ByteArrayOutputStream out) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bytearrayencoder'");
    }

    private Object integerencoder(Integer data, ByteArrayOutputStream out) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'integerencoder'");
    }

    private Object stringencoder(String data, ByteArrayOutputStream out) {
        out.write(data.length());
        out.write(':');
        out.write(data.getBytes(StandardCharsets.UTF_8));
        return out.toString();
    }
}