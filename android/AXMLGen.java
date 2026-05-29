import java.io.*;
import java.util.*;

public class AXMLGen {
    static String[] strings;
    
    public static void main(String[] args) throws Exception {
        strings = new String[]{
            "", "manifest", "xmlns:android",
            "http://schemas.android.com/apk/res/android",
            "application", "activity", "intent-filter", "action", "category",
            ".MainActivity", "MCC", "1.0.0",
            "android.intent.action.MAIN", "android.intent.category.LAUNCHER",
            "versionCode", "versionName", "label", "name", "exported",
            "configChanges", "windowSoftInputMode", "usesCleartextTraffic",
            "android", "allowBackup", "debuggable", "orientation|keyboardHidden|screenSize",
            "adjustResize"
        };
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        // XML header
        writeShort(bos, 0x0003);
        writeShort(bos, 8);
        writeInt(bos, 0); // size placeholder
        
        // String pool: type=0x0001, hdr=0x1C
        writeShort(bos, 0x0001);
        writeShort(bos, 0x1C);
        writeInt(bos, 0); // chunk size placeholder
        writeInt(bos, strings.length);
        writeInt(bos, 0); // styleCount
        writeInt(bos, 0x0100); // utf16
        writeInt(bos, 0); // stringsStart placeholder
        writeInt(bos, 0); // stylesStart
        
        int[] offsets = new int[strings.length];
        byte[][] strBytes = new byte[strings.length][];
        int offset = 0;
        for (int i = 0; i < strings.length; i++) {
            offsets[i] = offset;
            strBytes[i] = strings[i].getBytes("UTF-16LE");
            offset += 2 + strBytes[i].length + 2; // charCount + data + null
        }
        for (int off : offsets) writeInt(bos, off);
        for (int i = 0; i < strings.length; i++) {
            writeShort(bos, strings[i].length());
            bos.write(strBytes[i]);
            writeShort(bos, 0);
        }
        while (bos.size() % 4 != 0) bos.write(0);
        
        // Patch string pool sizes
        byte[] data = bos.toByteArray();
        int spSize = data.length - 12; // from after XML header
        setInt(data, 12 + 4, spSize); // chunkSize
        setInt(data, 12 + 20, 0x1C + strings.length * 4); // stringsStart
        
        // Resource map
        writeShort(bos, 0x0180);
        writeShort(bos, 8);
        int[] attrIds = {0x0101021b, 0x0101021c, 0x01010001, 0x01010003, 0x01010431,
                         0x0101009e, 0x010100d3, 0x010103ef, 0x01010280, 0x0101000f};
        writeInt(bos, 8 + attrIds.length * 4);
        for (int id : attrIds) writeInt(bos, id);
        
        // NS
        writeShort(bos, 0x0100); writeShort(bos, 16); writeInt(bos, 16);
        writeInt(bos, idx("android")); writeInt(bos, idx("http://schemas.android.com/apk/res/android"));
        
        // Manifest (hdrSize=16, no metadata)
        writeTag(bos, "manifest", new Attr[]{
            new Attr("http://schemas.android.com/apk/res/android", "versionCode", 0x10, 0, 1),
            new Attr("http://schemas.android.com/apk/res/android", "versionName", 0x03, idx("1.0.0"), 0),
        });
        writeTag(bos, "application", new Attr[]{
            new Attr("http://schemas.android.com/apk/res/android", "allowBackup", 0x12, -1, 0),
            new Attr("http://schemas.android.com/apk/res/android", "label", 0x03, idx("MCC"), 0),
            new Attr("http://schemas.android.com/apk/res/android", "usesCleartextTraffic", 0x12, -1, 0xFFFFFFFF),
            new Attr("http://schemas.android.com/apk/res/android", "debuggable", 0x12, -1, 0xFFFFFFFF),
        });
        writeTag(bos, "activity", new Attr[]{
            new Attr("http://schemas.android.com/apk/res/android", "name", 0x03, idx(".MainActivity"), 0),
            new Attr("http://schemas.android.com/apk/res/android", "exported", 0x12, -1, 0xFFFFFFFF),
            new Attr("http://schemas.android.com/apk/res/android", "configChanges", 0x03, idx("orientation|keyboardHidden|screenSize"), 0),
            new Attr("http://schemas.android.com/apk/res/android", "windowSoftInputMode", 0x03, idx("adjustResize"), 0),
        });
        writeTag(bos, "intent-filter", new Attr[]{});
        writeTag(bos, "action", new Attr[]{
            new Attr("http://schemas.android.com/apk/res/android", "name", 0x03, idx("android.intent.action.MAIN"), 0),
        });
        writeEnd(bos, "action");
        writeTag(bos, "category", new Attr[]{
            new Attr("http://schemas.android.com/apk/res/android", "name", 0x03, idx("android.intent.category.LAUNCHER"), 0),
        });
        writeEnd(bos, "category");
        writeEnd(bos, "intent-filter");
        writeEnd(bos, "activity");
        writeEnd(bos, "application");
        writeEnd(bos, "manifest");
        
        // END_NS
        writeShort(bos, 0x0101); writeShort(bos, 16); writeInt(bos, 16);
        writeInt(bos, idx("android")); writeInt(bos, idx("http://schemas.android.com/apk/res/android"));
        
        // Finalize
        byte[] axml = bos.toByteArray();
        setInt(axml, 4, axml.length);
        
        FileOutputStream fos = new FileOutputStream("build/axml_java.bin");
        fos.write(axml);
        fos.close();
        System.out.println("AXML: " + axml.length + " bytes");
    }
    
    static int idx(String s) {
        for (int i = 0; i < strings.length; i++) if (strings[i].equals(s)) return i;
        return -1;
    }
    
    static void writeShort(ByteArrayOutputStream bos, int v) {
        bos.write(v & 0xFF); bos.write((v >> 8) & 0xFF);
    }
    static void writeInt(ByteArrayOutputStream bos, int v) {
        bos.write(v & 0xFF); bos.write((v >> 8) & 0xFF);
        bos.write((v >> 16) & 0xFF); bos.write((v >> 24) & 0xFF);
    }
    static void setInt(byte[] data, int pos, int v) {
        data[pos] = (byte)(v & 0xFF);
        data[pos+1] = (byte)((v >> 8) & 0xFF);
        data[pos+2] = (byte)((v >> 16) & 0xFF);
        data[pos+3] = (byte)((v >> 24) & 0xFF);
    }
    
    static class Attr {
        String ns, name; int type, strIdx, data;
        Attr(String n, String na, int t, int si, int d) { ns=n; name=na; type=t; strIdx=si; data=d; }
    }
    
    static void writeTag(ByteArrayOutputStream bos, String tagName, Attr[] attrs) throws Exception {
        int na = attrs.length;
        writeShort(bos, 0x0102);
        writeShort(bos, 16); // hdrSize
        writeInt(bos, 16 + na * 20);
        writeInt(bos, -1); // ns
        writeInt(bos, idx(tagName));
        for (Attr a : attrs) {
            writeInt(bos, a.ns != null ? idx(a.ns) : -1);
            writeInt(bos, idx(a.name));
            writeInt(bos, a.strIdx);
            writeShort(bos, a.type);
            writeShort(bos, 0);
            writeInt(bos, a.data);
        }
    }
    
    static void writeEnd(ByteArrayOutputStream bos, String tagName) throws Exception {
        writeShort(bos, 0x0103);
        writeShort(bos, 16);
        writeInt(bos, 16);
        writeInt(bos, -1);
        writeInt(bos, idx(tagName));
    }
}
