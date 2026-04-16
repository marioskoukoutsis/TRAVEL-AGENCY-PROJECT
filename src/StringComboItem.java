public class StringComboItem {
    private String id;
    private String name;

    public StringComboItem(String id, String name) {
        this.id = id;
        this.name = name;
    }
    public String getId() { return id; }
    @Override
    public String toString() { return name; }
}
