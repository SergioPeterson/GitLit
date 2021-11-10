package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {
    private String id;
    private String name;
    private byte[] file;

    public Blob(String name) {
        this.name = name;
        this.file = Utils.readContents(new File(this.name));
        this.id = Utils.sha1(this.name, this.file);
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public byte[] getFile() {
        return file;
    }

}
