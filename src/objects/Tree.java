package objects;

import utils.Hash;
import utils.KeyValueStore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;

public class Tree extends KVObject {

    // 存放tree对象包含的objects的arraylist
    private final ArrayList<KVObject> objects = new ArrayList<>();

    // 根据路径创建tree对象
    public Tree(String path) {
        objectType = "tree";
        filePath = path;
        filename = filePath.substring(filePath.lastIndexOf(File.separator));

    }

    // 根据File对象创建tree对象,并在tree对象的objects数组中存入file对应的目录下的所有文件/文件夹
    public Tree(File file) {
        objectType = "tree";
        filePath = file.getAbsolutePath();
        filename = file.getName();

        // 计算tree对象的hash值
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
            Hash.dirHash(new File(filePath), md);
            key = Hash.digest(md);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 计算tree对象的hash值
    private void updateHashcode() {
        ArrayList<File> fileArrayList = new ArrayList<>();
        for (KVObject object : objects) {
            if (object.isTree()) {
                Tree tree = (Tree) object;
                fileArrayList.add(new File(tree.getFilePath()));
            }
            if (object.isBlob()) {
                Blob blob = (Blob) object;
                fileArrayList.add(new File(blob.getFilePath()));
            }
        }

        fileArrayList.add(new File(filePath));
        File[] objectsFiles = fileArrayList.toArray(new File[fileArrayList.size()]);
        key = Hash.fileArrayHash(objectsFiles);
    }

    public void addObject(KVObject object) {
        objects.add(object);
        updateHashcode();
    }

    public void clearObjects() {
        objects.clear();
    }

    public boolean containsObjects() {
        if (objects.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    public ArrayList<KVObject> getObjects() {
        return objects;
    }

    @Override
    public void store() {
        String objectFilePath;
        try {
            objectFilePath = KeyValueStore.createObjectFile(this.getKey());
            // 在存储文件中写入tree包含的objects的信息
            BufferedWriter out = new BufferedWriter(new FileWriter(objectFilePath));
            for (KVObject i : objects) {
                out.write(i.toString() + System.getProperty("line.separator"));
                out.flush();
                if (i.isTree()) {
                    i.store();
                }
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 给定文件目录，将目录下的文件转化为tree和blob,并将blob保存
    public void convert() throws IOException {
        File dir = new File(filePath);
        File[] files = dir.listFiles();
        for (File i : files) {
            if (i.isDirectory()) {
                Tree tree = new Tree(i);
                this.addObject(tree);
                tree.convert();
            }
            if (i.isFile()) {
                Blob blob = new Blob(i);
                this.addObject(blob);
                blob.store();
            }
        }
    }

    @Override
    public String toString() {
        return objectType + " " + key + "\t" + filename;
    }
}
