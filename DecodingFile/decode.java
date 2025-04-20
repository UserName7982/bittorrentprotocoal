package DecodingFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class decode {
    private int index = 0;
    private final byte[] data;

    public decode(byte[] data) {
        this.data = data;
    }

    /**
     * Decodes the current position of the byte array based on the indicator byte.
     * The decoding scheme is as follows:
     * - If the indicator is 'i', it decodes an integer.
     * - If the indicator is 'l', it decodes a list.
     * - If the indicator is 'd', it decodes a map.
     * - Otherwise, it decodes a string.
     *
     * @return the decoded object (integer, list, map, or string) or null if data is
     *         invalid
     */
    public Object Decode() {
        if (index >= data.length) {
            Throwable t = new Throwable("invalid data");
            t.printStackTrace();
            return null;
        }
        byte indicator = data[index];
        if (indicator == 'i') {
            return IntegerDecoder();
        } else if (indicator == 'l') {
            return ListDecoder();
        } else if (indicator == 'd') {
            return MapDecoder();
        } else {
            return StringDecoder();
        }
    }

    /**
     * Decodes a string from the current position of the byte array. The decoding
     * scheme is as follows:
     * - The length of the string is read from the byte array as a decimal
     * number, followed by a colon.
     * - The string is read from the byte array with the given length.
     * 
     * @return the decoded string
     */
    private String StringDecoder() {
        int colonPos = index;
        while (data[colonPos] != ':') {
            colonPos++;
        }
        int length = Integer.parseInt(new String(data, index, colonPos - index, StandardCharsets.UTF_8));
        index = colonPos + 1;
        String str = new String(data, index, length, StandardCharsets.UTF_8);
        index += length;
        return str;
    }

    /**
     * Decodes a map from the current position of the byte array. The decoding
     * scheme is as follows:
     * - The type byte 'd' is read from the byte array.
     * - The keys of the map are decoded as strings until the type byte 'e' is
     * encountered.
     * - For each key, the value is decoded recursively using this class's Decode
     * method.
     * - The type byte 'e' is read from the byte array, marking the end of the map.
     * 
     * @return the decoded map
     */
    private Map<String, ?> MapDecoder() {
        Map<String, Object> map = new HashMap<>();
        index++;
        while (data[index] != 'e') {
            String key = StringDecoder();
            if (key.equals("pieces") || key.equals("info_hash") || key.equals("peer_id")) {
                map.put(key, ByteStringDecoder());
            } else {
                map.put(key, Decode());
            }
        }
        index++;
        return map;
    }

    /**
     * Decodes a list from the current position of the byte array. The decoding
     * scheme is as follows:
     * - The type byte 'l' is read from the byte array.
     * - Each element of the list is decoded recursively using this class's Decode
     * method.
     * - The type byte 'e' is read from the byte array, marking the end of the list.
     * 
     * @return the decoded list
     */
    private List<?> ListDecoder() {
        List<Object> list = new ArrayList<>();
        index++;
        while (data[index] != 'e') {
            list.add(Decode());
        }
        index++;
        return list;
    }

    /**
     * Decodes an integer from the current position of the byte array. The decoding
     * scheme is as follows:
     * - The type byte 'i' is read from the byte array.
     * - The string representation of the integer is read from the byte array.
     * - The type byte 'e' is read from the byte array, marking the end of the
     * integer.
     * 
     * @return the decoded integer
     */
    private Long IntegerDecoder() {
        index++;
        int epos = index;
        while (data[epos] != 'e') {
            epos++;
        }
        long integer = Long.parseLong(new String(data, index, epos - index, StandardCharsets.UTF_8));
        index = epos + 1;
        return integer;
    }

    /**
     * Decodes a byte string from the current position of the byte array. The
     * decoding
     * scheme is as follows:
     * - The length of the byte string is read from the byte array as a decimal
     * number, followed by a colon.
     * - The byte string is read from the byte array with the given length.
     * 
     * @return the decoded byte string
     */
    private byte[] ByteStringDecoder() {
        int colonPos = index;

        // Ensure we do not go out of bounds
        while (colonPos < data.length && data[colonPos] != ':') {
            colonPos++;
        }

        // If colon not found or index exceeds array bounds
        if (colonPos >= data.length) {
            throw new RuntimeException("Colon not found while decoding byte string");
        }

        // Extract and validate length string
        String lengthStr = new String(data, index, colonPos - index, StandardCharsets.UTF_8);

        int length;
        try {
            length = Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid length for byte string: " + lengthStr, e);
        }
        index = colonPos + 1;
        // Check bounds before copying
        if (index + length > data.length) {
            throw new RuntimeException("Byte string length exceeds data bounds");
        }

        byte[] result = new byte[length];
        System.arraycopy(data, index, result, 0, length);
        index += length;
        return result;
    }
}
