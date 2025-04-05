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
 *  - If the indicator is 'i', it decodes an integer.
 *  - If the indicator is 'l', it decodes a list.
 *  - If the indicator is 'd', it decodes a map.
 *  - Otherwise, it decodes a string.
 *
 * @return the decoded object (integer, list, map, or string) or null if data is invalid
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

    private String StringDecoder() {
        int colonPos = index;
        while (data[colonPos] != ':') {
            colonPos++;
        }
        int length = Integer.parseInt(new String(data, index, colonPos - index, StandardCharsets.UTF_8));
        index = colonPos +1;
        String str = new String(data, index, length, StandardCharsets.UTF_8);
        index += length;
        return str;
    }

    private Map<String, ?> MapDecoder() {
        Map<String, Object> map = new HashMap<>();
        index++;
        while (data[index] != 'e') {
            String key = StringDecoder();
            map.put(key, Decode());
        }
        index++;
        return map;
    }

    private List<?> ListDecoder() {
        List<Object> list = new ArrayList<>();
        index++;
        while (data[index] != 'e') {
            list.add(Decode());
        }
        index++;
        return list;
    }

    private Long IntegerDecoder() {
        index++;
        int epos = index;
        while (data[epos] != 'e') {
            epos++;
        }
        long integer = Long.parseLong(new String(data, index, epos-index, StandardCharsets.UTF_8));
        index=epos+1;
        return integer;
    }
}
