package it.acsoftware.hyperiot.hbase.connector.model;

/**
 * This enum contains column families of an HBase table, which is dedicated to timeline queries.
 * Remember: there is one table per project.
 */
public enum HBaseTimelineColumnFamily {

    YEAR("year", 0), MONTH("month", 1), DAY("day", 2), HOUR("hour", 3),
    MINUTE("minute", 4), SECOND("second", 5), MILLISECOND("millisecond", 6);

    private String name;
    private int order;

    HBaseTimelineColumnFamily(String name, int order) {
        this.name = name;
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

}
