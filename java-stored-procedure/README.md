# SAP HANA High-Performance SP Invoker

## Overview
This Java utility is designed for Mule 4 applications that require sub-millisecond overhead when calling SAP HANA Stored Procedures. It is specifically optimized for cases where the standard Mule Database Connector fails to handle HANA Table Type returns or where latency must be minimized.

## Performance Features
* **HikariCP Pooling:** Eliminates the 2-3 second TCP/TLS handshake delay on every request.
* **Jackson Streaming:** Bypasses `List<Map>` creation, reducing GC pressure and memory allocation.
* **Positional Fetching:** Uses fixed column indexes to skip `ResultSetMetaData` network round-trips.
* **Thread Safety:** Designed for highly concurrent Mule 4 "Uber" thread pools without `synchronized` bottlenecks.

## Prerequisites
Ensure the following dependencies are in your `pom.xml`:
- `com.zaxxer:HikariCP:5.1.0`
- `com.fasterxml.jackson.core:jackson-core:2.15.x`
- `com.sap.cloud.db.jdbc:ngdbc` (SAP HANA JDBC Driver)

## Configuration

### 1. Externalize Columns
Define the expected output columns in your `config-xxx.yaml`. **The order must match the HANA Stored Procedure result table exactly.**

```yaml
hana:
  out_columns: "REQUEST,SESSION_ID,ESIID,BPARTNER,UCCONTRACT,ACTIONS,CHANNEL"
