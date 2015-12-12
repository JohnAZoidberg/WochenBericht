package de.struckmeierfliesen.ds.wochenbericht;


public class Client {
    public int id;
    public String name;

    public int tel = -1;
    public String adress = null;

    public Client(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
