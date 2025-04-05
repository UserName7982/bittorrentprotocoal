package EncodingFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DecodingFile.decode;
public class endcode {
    // String encode <length>:<string>
    // integer encode i:<integer>e
    // List encode l:<item1>...<itemN>e
    // Map<String,Object> encode d:<key1>:<value1>...<keyN>:<valueN>e
    // byte[] encode <length>:<data>

    /**
     * Encodes the given object into a byte array based on the encoding scheme
     * outlined in the class documentation
     *
     * @param data the object to encode
     * @return a byte array containing the encoded data
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public byte[] encode(Object data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (data == null) {
            Throwable t = new Throwable("Null data cannot be encoded");
            System.out.println(t.getMessage());
            return null;
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

    /**
     * Encodes the given Map into a byte array. The encoding scheme is as follows:
     *  - The type byte 'd' is written to the output stream
     *  - The keys of the map are sorted and for each key, the key and its value
     *    are encoded (using this class's encode method) and written to the output
     *    stream
     *  - The type byte 'e' is written to the output stream
     * @param data the Map to encode
     * @param out the output stream to write the encoded data to
     * @return a byte array containing the encoded data
     * @throws IOException
     */
    private byte[] mapencoder(Map<String, Object> data, ByteArrayOutputStream out) {
       out.write('d');
       List<String> list=new ArrayList<>(data.keySet());
       Collections.sort(list);
       for (String key : list) {
           try {
               out.write(encode(key));
               out.write(encode(data.get(key)));
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
       out.write('e');
       return out.toByteArray();
    }
    /**
     * Encodes the given List into a byte array. The encoding scheme is as follows:
     *  - The type byte 'l' is written to the output stream
     *  - Each element of the list is encoded (using this class's encode method) and
     *    written to the output stream
     *  - The type byte 'e' is written to the output stream
     * @param data the List to encode
     * @param out the output stream to write the encoded data to
     * @return a byte array containing the encoded data
     * @throws IOException
     */
    private byte[] listencoder(List<?> data, ByteArrayOutputStream out) throws IOException {
        out.write('l');
        for (Object object : data) {
            out.write(encode(object));
        }

        out.write('e');
        return out.toByteArray();
    }

    /**
     * Encodes the given byte array into a byte array. The encoding scheme is as follows:
     *  - The length of the byte array (in bytes) is written to the output stream as a string
     *  - The type byte ':' is written to the output stream
     *  - The bytes of the byte array are written to the output stream
     * @param data the byte array to encode
     * @param out the output stream to write the encoded data to
     * @return a byte array containing the encoded data
     * @throws IOException
     */
    private byte[] bytearrayencoder(byte[] data, ByteArrayOutputStream out) throws IOException {
        byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);
        out.write(String.valueOf(bytes.length).getBytes(StandardCharsets.UTF_8));
        out.write(':');
        out.write(data);
        return out.toByteArray();
    }

    /**
     * Encodes the given integer into a byte array. The encoding scheme is as follows:
     *  - The type byte 'i' is written to the output stream
     *  - The string representation of the integer is written to the output stream
     *  - The type byte 'e' is written to the output stream
     * @param data the integer to encode
     * @param out the output stream to write the encoded data to
     * @return a byte array containing the encoded data
     * @throws IOException
     */
    private byte[] integerencoder(Integer data, ByteArrayOutputStream out) throws IOException {
       out.write('i');
       out.write(data.toString().getBytes(StandardCharsets.UTF_8));
       out.write('e');
       return out.toByteArray();
    }

    /**
     * Encodes the given string into a byte array. The encoding scheme is as follows:
     *  - The length of the string (in bytes) is written to the output stream as a string
     *  - The type byte ':' is written to the output stream
     *  - The bytes of the string are written to the output stream
     * @param data the string to encode
     * @param out the output stream to write the encoded data to
     * @return a byte array containing the encoded data
     * @throws IOException
     */

    private byte[] stringencoder(String data, ByteArrayOutputStream out) throws IOException {
        byte[] bytes=data.getBytes(StandardCharsets.UTF_8);
        out.write(String.valueOf(bytes.length).getBytes(StandardCharsets.UTF_8));
        out.write(':');
        out.write(bytes);
        return out.toByteArray();
    }
    public static void main(String[] args) {
        endcode encode=new endcode();
        byte[] encoded=null;
        Map<String,Object> map=new HashMap<>();
        map.put("announcer", "http//example.com/announce");
        map.put("pieces", 1222);
        map.put("length", 33);
        map.put("info", "infohash");
        try {
            encoded = encode.encode(map);
            System.out.println("Encoded data: " + new String(encoded));
        } catch (IOException e) {
            e.printStackTrace();
        }
        decode decode=new decode(encoded);
        Object decoded = decode.Decode();
        System.out.println("Decoded data: " + decoded);
    }
}
