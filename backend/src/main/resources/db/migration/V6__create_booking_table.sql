-- Create booking table with multi-tenant isolation and capacity management
CREATE TABLE booking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    dog_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    notes TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Foreign keys
    CONSTRAINT fk_booking_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_dog FOREIGN KEY (dog_id) REFERENCES dog(id) ON DELETE CASCADE,
    
    -- Business rule: end date must be after start date
    CONSTRAINT chk_booking_dates CHECK (end_date > start_date)
);

-- Index for tenant isolation queries
CREATE INDEX idx_booking_tenant_id ON booking(tenant_id);

-- Index for date range queries (availability checks)
CREATE INDEX idx_booking_tenant_start_date ON booking(tenant_id, start_date);

-- Index for dog booking history
CREATE INDEX idx_booking_tenant_dog ON booking(tenant_id, dog_id);

-- Index for customer booking history
CREATE INDEX idx_booking_tenant_customer ON booking(tenant_id, customer_id);

-- Comments
COMMENT ON TABLE booking IS 'Bookings for dogs with capacity management and multi-tenant isolation';
COMMENT ON COLUMN booking.tenant_id IS 'Tenant owning this booking';
COMMENT ON COLUMN booking.start_date IS 'Booking start date (inclusive)';
COMMENT ON COLUMN booking.end_date IS 'Booking end date (exclusive, like hotel checkout)';
COMMENT ON COLUMN booking.status IS 'Booking status: CONFIRMED, CANCELLED';
COMMENT ON CONSTRAINT chk_booking_dates ON booking IS 'Ensure end date is after start date';
