package it.cinofilo.domain.entitlement;

public final class Entitlements {

    private Entitlements() {}

    public static final BooleanEntitlement BOOKING_MANAGEMENT   = new BooleanEntitlement("BOOKING_MANAGEMENT");
    public static final BooleanEntitlement CUSTOMER_MANAGEMENT  = new BooleanEntitlement("CUSTOMER_MANAGEMENT");
    public static final BooleanEntitlement DOG_MANAGEMENT       = new BooleanEntitlement("DOG_MANAGEMENT");
    public static final BooleanEntitlement CALENDAR_VIEW        = new BooleanEntitlement("CALENDAR_VIEW");
    public static final BooleanEntitlement SMS_NOTIFICATIONS    = new BooleanEntitlement("SMS_NOTIFICATIONS");
    public static final BooleanEntitlement API_ACCESS           = new BooleanEntitlement("API_ACCESS");
    public static final BooleanEntitlement ADVANCED_REPORTS     = new BooleanEntitlement("ADVANCED_REPORTS");

    public static final QuotaEntitlement MAX_STAFF_USERS        = new QuotaEntitlement("MAX_STAFF_USERS");
    public static final QuotaEntitlement MAX_DOGS_PER_TENANT    = new QuotaEntitlement("MAX_DOGS_PER_TENANT");
    public static final QuotaEntitlement MAX_BOOKINGS_PER_MONTH = new QuotaEntitlement("MAX_BOOKINGS_PER_MONTH");
}
