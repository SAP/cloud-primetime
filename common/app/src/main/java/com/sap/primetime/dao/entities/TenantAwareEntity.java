package com.sap.primetime.dao.entities;

import javax.persistence.MappedSuperclass;

import org.eclipse.persistence.annotations.Multitenant;
import org.eclipse.persistence.annotations.TenantDiscriminatorColumn;

/**
 * Tenant enabling for tables which depend on tenants.
 *
 */
@MappedSuperclass
@Multitenant
@TenantDiscriminatorColumn(contextProperty = "eclipselink.tenant-id", length = 50)
public abstract class TenantAwareEntity extends BasicEntity {
}