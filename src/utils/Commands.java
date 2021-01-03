package utils;

import objects.Blob;
import objects.Tree;
import objects.Commit;
import refs.Branch;
import refs.Head;

import java.io.File;
import java.io.IOException;

public class Commands {
    // 用于存放add过程中已添加的object的arraylist
    private static Tree rootTree;

    private Commands() {
    }

    public static void setRootTree(String rootPath) {
        rootTree = new Tree(rootPath);
    }

    // 根据绝对路径设置工作区目录的方法
    public static void setWorkingDirectory(String absolutePath) {
        KeyValueStore.setWorkingDirectory(absolutePath);
    }

    public static void initialize() {
        if (KeyValueStore.getWorkingDirectory() == null) {
            System.out.println("failure: Working directory has not been set. Use \"wvc set\" to set working directory.");
            return;
        }
        // 判断当前工作区是否已经初始化
        File file = new File(KeyValueStore.getWorkingDirectory() + File.separator + "wvc");
        if (file.exists()) {
            System.out.println("Reinitialized existing Wvc repository in " + KeyValueStore.getWorkingDirectory() + KeyValueStore.getWvcRootPath());
            return;
        }

        setRootTree(KeyValueStore.getWorkingDirectory());
        // 创建wvc文件目录
        KeyValueStore.makeDirs(KeyValueStore.getRefsPath());
        KeyValueStore.makeDirs(KeyValueStore.getLogsPath());
        KeyValueStore.makeDirs(KeyValueStore.getObjectsPath());
        // 创建HEAD文件
        try {
            KeyValueStore.createFile(Head.getHeadPath());
//            Head.update();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void add(String relativePath) {
        String filePath = KeyValueStore.getWorkingDirectory() + File.separator + relativePath;

        // add的文件不存在时的处理
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("failure: '" + filePath + "' did not match any files.");
            return;
        }
        // 若add的对象为文件，则直接转化为blob存储
        if (file.isFile()) {
            Blob blob = new Blob(file);
            rootTree.getObjects().add(blob);
            blob.store();
        }
        // 若add的对象为文件夹，则转化为tree，并递归地存储该文件夹下的文件/文件夹
        if (file.isDirectory()) {
            Tree subTree = new Tree(file);
            rootTree.getObjects().add(subTree);
            try {
                subTree.convert();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void commit(String message) {
        Commit newCommit = new Commit(rootTree);
        newCommit.setMessage(message);
        // 当前提交为初次提交的处理
        if (Head.getWorkingBranch() == null) {
            try {
                Branch mainBranch = new Branch("main");
                mainBranch.setPointTo(newCommit.getKey());
                Head.setWorkingBranch(mainBranch);
                Head.update();
                mainBranch.store();
                newCommit.store();
                rootTree.getObjects().clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                String parentCommitID = new Branch(Head.getWorkingBranch()).getAssociatedCommitID();
                String parentTreeID = KeyValueStore.readFileContent(KeyValueStore.getObjectsPath() + parentCommitID.substring(0, 2) + File.separator + parentCommitID.substring(2)).substring(5, 46);
                // 判断本次提交是否有文件发生改动，将本次提交的tree hashcode和上次提交的tree hashcode进行比较，若相同则拒绝提交
                if (newCommit.getTreeID() == parentTreeID) {
                    System.out.println("failure: No files changed.");
                    return;
                }
                Branch existingBranch = new Branch(Head.getWorkingBranch());
                newCommit.setParent(existingBranch.getAssociatedCommitID());
                existingBranch.setPointTo(newCommit.getKey());
                existingBranch.update();
                newCommit.store();
                rootTree.getObjects().clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void branch(String branchName) {
        File file = new File(KeyValueStore.getRefsPath() + branchName);
        if (file.exists()) {
            System.out.println("failure: A branch named '" + branchName + "' already exists.");
        }
        try {
            Branch newBranch = new Branch(branchName);
            Branch currentBranch = new Branch(Head.getWorkingBranch());
            newBranch.setPointTo(currentBranch.getAssociatedCommitID());
            newBranch.store();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 更改当前工作的分支
    public static void checkOut(String branchName) {
        File file = new File(KeyValueStore.getRefsPath() + branchName);
        if (!file.exists()) {
            System.out.println("failure: Branch '" + branchName + "' did not match any existing branches");
            return;
        }
        try {
            Head.setWorkingBranch(new Branch(branchName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String listBranches() {
        String listOfBranches = null;
        File[] files = new File(KeyValueStore.getRefsPath()).listFiles();
        for (File file : files) {
            listOfBranches += file.getName() + "\n";
        }
        return listOfBranches;
    }

    public static String listFiles() {
        String listOfFiles = null;
        if (KeyValueStore.getWorkingDirectory() == null) {
            System.out.println("failure: Working directory has not been set.");
            return listOfFiles;
        }
        File[] files = new File(KeyValueStore.getWorkingDirectory()).listFiles();
        for (File file : files) {
            listOfFiles += file.getName();
        }
        return listOfFiles;
    }

    //    public static String log() {
//
//    }

    public static void reset(String partialHashCodeOfCommit) {

    }
}
