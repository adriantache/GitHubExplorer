package com.adriantache.githubexplorer;

/**
 * Holds a link to a file or directory inside a GitHub repository
 **/
public class Link {
    private String name;
    private String path;
    private String type;

    Link(String name, String path, String type){
        this.name = name;
        this.path = path;
        this.type = type;
    }

    public String getName() {
        return name;
    }
    public String getPath() {
        return path;
    }
    public String getType() {
        return type;
    }
}
