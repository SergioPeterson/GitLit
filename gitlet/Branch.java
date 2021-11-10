package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;
import java.util.HashSet;

import static gitlet.Utils.join;
import static gitlet.Utils.readObject;

public class Branch implements Serializable {

    public static final File BRANCHES_CWD = join(Repository.GITLET_DIR, "branches");
    private TreeMap<String, Blob> stageAdd = new TreeMap<>();
    private TreeMap<String, Blob> stageRemove = new TreeMap<>();
    private HashSet<String> remved = new HashSet<>();
    private TreeMap<String, String> allBlobs = new TreeMap<>();
    private TreeMap<String, Commit> commits = new TreeMap<>();
    private Commit latestCommit;
    private Branch currentBranch;
    private String name;
    private boolean merged = false;
    private String oldBranch;

    public Branch(String branch) {
        boolean exits = Utils.join(BRANCHES_CWD, branch).exists();
        if (exits) {
            currentBranch = getCurrentBranch(branch);
        } else {
            currentBranch = this;
        }
        currentBranch.name = branch;
    }

    public Branch(String branch, Branch oldBranch) {
        boolean exits = Utils.join(BRANCHES_CWD, branch).exists();
        if (exits) {
            currentBranch = getCurrentBranch(branch);
        } else {
            currentBranch = this;
        }
        currentBranch.name = branch;
        currentBranch.stageAdd.putAll(oldBranch.stageAdd);
        currentBranch.stageRemove.putAll(oldBranch.stageRemove);
        currentBranch.remved = (HashSet<String>) oldBranch.remved.clone();
        currentBranch.allBlobs.putAll(oldBranch.allBlobs);
        currentBranch.commits.putAll(oldBranch.commits);
        currentBranch.latestCommit = oldBranch.latestCommit;
    }

    public void makeAdd(String file) {
        Blob newBlob = new Blob(file);
        if (currentBranch.stageRemove.containsKey(newBlob.getID())) {
            currentBranch.stageRemove.remove(newBlob.getID());
            currentBranch.remved.remove(newBlob.getName());
        } else if (!tracked(newBlob.getID())
                && !currentBranch.stageAdd.containsKey(newBlob.getID())) {
            currentBranch.stageAdd.put(newBlob.getID(), newBlob);
            currentBranch.allBlobs.put(newBlob.getID(), newBlob.getName());
        }
        currentBranch.saveBranch();
    }

    public void makeCommit() {
        Commit newCommit = new Commit();
        currentBranch.commits.put(newCommit.getID(), newCommit);
        newCommit.saveCommit();
        currentBranch.latestCommit = newCommit;
    }

    public void makeCommit(String message) {
        if (currentBranch.stageAdd.size() == 0 && currentBranch.stageRemove.size() == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        Commit newCommit = new Commit(message, currentBranch.latestCommit);
        newCommit.addBlobs(currentBranch.stageAdd, currentBranch.stageRemove);
        currentBranch.stageAdd.clear();
        currentBranch.stageRemove.clear();
        currentBranch.remved.clear();
        currentBranch.commits.put(newCommit.getID(), newCommit);
        newCommit.saveCommit();
        currentBranch.latestCommit = newCommit;
        currentBranch.saveBranch();
    }

    public void remove(String file) {
        File oldFile = join(Repository.CWD, file);
        boolean ran = false;
        if (oldFile.exists()) {
            Blob newBlob = new Blob(file);
            if (currentBranch.stageAdd.containsKey(newBlob.getID())) {
                currentBranch.stageAdd.remove(newBlob.getID());
                ran = true;
            } else if (tracked(newBlob.getID())) {
                currentBranch.stageRemove.put(newBlob.getID(), newBlob);
                currentBranch.remved.add(newBlob.getName());
                ran = true;
                oldFile.delete();
            }
            if (!ran) {
                System.out.println("No reason to remove the file.");
                System.exit(0);
                return;
            }
            currentBranch.saveBranch();
        } else {
            remved.add(file);
        }
    }

    public void log() {
        Commit holder = currentBranch.latestCommit;
        while (holder != null) {
            if (merged) {
                printlog(holder.getID(), holder.getTime(), holder.getMessage(),
                        currentBranch.oldBranch, currentBranch.latestCommit.getParent().getID());
                currentBranch.merged = false;
            } else {
                printlog(holder.getID(), holder.getTime(), holder.getMessage());
            }
            System.out.println();
            holder = holder.getParent();
        }
    }


    public void find(String message) {
        boolean found = false;
        File commitFolder = new File(Commit.COMMIT_CWD.getPath());
        for (File file : commitFolder.listFiles()) {
            Commit commit = Utils.readObject(file, Commit.class);
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.getID());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    public void printStatus() {
        System.out.println("=== Staged Files ===");
        for (String file : currentBranch.stageAdd.keySet()) {
            String n = currentBranch.stageAdd.get(file).getName();
            System.out.println(n);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String thisname : currentBranch.remved) {
            System.out.println(thisname);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public void checkout(File fileName) {
        Commit commit = currentBranch.latestCommit;
        TreeMap<String, Blob> blobs = commit.getBlobs();
        for (String blob : blobs.keySet()) {
            Blob file = blobs.get(blob);
            if (file.getName().equals(fileName.getName())) {
                restoreBlob(file);
            }
        }
        currentBranch.saveBranch();

    }

    public void checkout(String id, String fileName) {
        boolean hasid = false;
        File commitFolder = new File(Commit.COMMIT_CWD.getPath());
        for (File thiscommits : commitFolder.listFiles()) {
            Commit commit = Utils.readObject(thiscommits, Commit.class);
            if (commit.getID().equals(id) || commit.getID().substring(0, 8).equals(id)) {
                for (String blob : commit.getBlobs().keySet()) {
                    Blob file = commit.getBlobs().get(blob);
                    if (file.getName().equals(fileName)) {
                        restoreBlob(file);
                        currentBranch.saveBranch();
                        return;
                    }
                }
                System.out.println("File does not exist in that commit");
                System.exit(0);
                return;
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
    }

    public void setFiles() {
        clear();
        for (Blob blob : currentBranch.latestCommit.getBlobs().values()) {
            restoreBlob(blob);
        }
        currentBranch.saveBranch();
    }

    public void setFiles(Commit commit) {
        clear();
        for (Blob blob : commit.getBlobs().values()) {
            restoreBlob(blob);
        }
        currentBranch.saveBranch();
    }

    public void delete() {
        File branch = join(BRANCHES_CWD, currentBranch.name);
        branch.delete();
    }

    private void restoreBlob(Blob blob) {
        File file = join(Repository.CWD, blob.getName());
        if (file.exists()) {
            file.delete();
        }
        Utils.writeContents(file, blob.getFile());
    }

    private void printlog(String shar, String date, String message) {
        System.out.println("===");
        System.out.println("commit " + shar);
        System.out.println("Date: " + date);
        System.out.println(message);
    }

    private void printlog(String shar, String date, String message, String other, String current) {
        System.out.println("===");
        System.out.println("commit " + shar);
        System.out.println("Merge: " + current.substring(0, 7) + " " + other.substring(0, 7));
        System.out.println("Date: " + date);
        System.out.println(message);
    }

    public Branch getCurrentBranch(String branchName) {
        File branch = join(BRANCHES_CWD, branchName);
        if (branch.exists()) {
            return readObject(branch, Branch.class);
        }
        throw new IllegalArgumentException("Branch does not exits");
    }

    public void saveBranch() {
        File newCommit = Utils.join(BRANCHES_CWD, this.name);
        Utils.writeObject(newCommit, this);
    }

    public void test() {
        System.out.println("\t-----------------------------------------------------------------");
        System.out.println("\tCurrent Stage for addition has :");
        for (String thisname : currentBranch.stageAdd.keySet()) {
            String value = currentBranch.stageAdd.get(thisname).getName();
            System.out.println("\tBlob : " + thisname + " Name : " + value);
        }
        System.out.println("\t-----------------------------------------------------------------\n");
        System.out.println("\n");
        System.out.println("\t-----------------------------------------------------------------");
        System.out.println("\tCurrent Stage for remove has :");
        for (String thisname : currentBranch.stageRemove.keySet()) {
            String value = currentBranch.stageRemove.get(thisname).getName();
            System.out.println("\tBlob : " + thisname + " Name : " + value);
        }
        System.out.println("\t-----------------------------------------------------------------\n");
        System.out.println("\n");
        System.out.println("\t-----------------------------------------------------------------");
        System.out.println("\tAll the Blobs in memory are :");
        int counter = 1;
        for (String id : currentBranch.allBlobs.keySet()) {
            String thisname = currentBranch.allBlobs.get(id);
            System.out.println("\tBlob " + counter + " : " + id + " also named " + thisname);
            counter++;
        }
        System.out.println("\t-----------------------------------------------------------------\n");
        System.out.println("\n");
        System.out.println("\t-----------------------------------------------------------------");
        System.out.println("\tCurrent Commits are :");
        for (String thisname : currentBranch.commits.keySet()) {
            Commit value = currentBranch.commits.get(thisname);
            System.out.println();
            value.info();
        }
        System.out.println("\t-----------------------------------------------------------------\n");
        System.out.println("\n");
        System.out.println("\t-----------------------------------------------------------------");
        System.out.println("\tLatest Commit was:");
        System.out.println("\t" + currentBranch.latestCommit.getMessage() + " with ID : "
                + currentBranch.latestCommit.getID());
        System.out.println("\t-----------------------------------------------------------------\n");
    }

    public void reset(String id) {
        for (File file : Repository.CWD.listFiles()) {
            if (file.getName().equals(".gitlet")
                    || file.getName().equals("gitlet")) {
                continue;
            }
            Blob blob = new Blob(file.getName());
            if (!currentBranch.tracked(blob.getID())
                    && (!currentBranch.stageAdd.containsKey(blob.getID())
                    || !currentBranch.stageRemove.containsKey(blob.getID()))) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                System.exit(0);
                return;
            }
        }
        for (String oldid : currentBranch.commits.keySet()) {
            if (oldid.equals(id)) {
                Commit commit = currentBranch.commits.get(oldid);
                currentBranch.latestCommit = commit;
                setFiles(commit);
                return;

            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
    }

    private void clear() {
        for (File file : Repository.CWD.listFiles()) {
            if (file.getName().equals(".gitlet")
                    || file.getName().equals("gitlet")) {
                continue;
            } else {
                file.delete();
            }
        }
    }

    public boolean tracked(String id) {
        return currentBranch.latestCommit.hasBlob(id);
    }

    public String getName() {
        return name;
    }

    public Commit getLatestCommit() {
        return currentBranch.latestCommit;
    }

    public boolean hasStuff() {
        return currentBranch.stageAdd.size() != 0 || currentBranch.stageRemove.size() != 0;
    }

    public boolean hasCommit(String id) {
        for (String commit : commits.keySet()) {
            if (commit.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public void setAsMerge(String other) {
        currentBranch.oldBranch = other;
        currentBranch.merged = true;
    }

    public void mergeCommit(String other, String oldID) {
        String message = "Merged " + other + " into " + currentBranch.name;
        Commit newCommit = new Commit(message, currentBranch.latestCommit);
        newCommit.addBlobs(currentBranch.stageAdd, currentBranch.stageRemove);
        currentBranch.merged = true;
        currentBranch.oldBranch = oldID;
        currentBranch.stageAdd.clear();
        currentBranch.stageRemove.clear();
        currentBranch.remved.clear();
        currentBranch.commits.put(newCommit.getID(), newCommit);
        newCommit.saveCommit();
        currentBranch.latestCommit = newCommit;
        currentBranch.saveBranch();
    }
}
