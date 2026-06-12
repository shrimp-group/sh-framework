# File and Network Utilities

<cite>
**Referenced Files in This Document**
- [FileUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/FileUtil.java)
- [CompressUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/CompressUtil.java)
- [NetworkUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/NetworkUtil.java)
- [QrCodeUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/QrCodeUtil.java)
- [PropertiesUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/PropertiesUtil.java)
- [ServerStateUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/ServerStateUtil.java)
- [IpHelper.java](file://sh-web/src/main/java/com/wkclz/web/helper/IpHelper.java)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)

## Introduction
This document provides comprehensive documentation for file system and network operation utilities within the framework. It covers file utilities for reading, writing, copying, moving, and managing files and directories; compression utilities for ZIP, GZIP, and other archive formats; network utilities for IP address detection, port scanning, URL handling, and HTTP operations; QR code generation and parsing utilities; properties file utilities for configuration management; and server state monitoring utilities for health checks and resource usage tracking. Practical examples demonstrate batch file operations, secure file transfers, network diagnostics, and automated backup processes.

## Project Structure
The utilities are primarily located in the sh-tool module under the utils package. Supporting network utilities are integrated via the sh-web module's IP helper. The following diagram illustrates the relationship between the relevant components.

```mermaid
graph TB
subgraph "sh-tool Module"
FU["FileUtil.java"]
CU["CompressUtil.java"]
NU["NetworkUtil.java"]
QRU["QrCodeUtil.java"]
PRU["PropertiesUtil.java"]
SSU["ServerStateUtil.java"]
end
subgraph "sh-web Module"
IPH["IpHelper.java"]
end
NU --> IPH
SSU --> FU
CU --> FU
```

**Diagram sources**
- [FileUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/FileUtil.java)
- [CompressUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/CompressUtil.java)
- [NetworkUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/NetworkUtil.java)
- [QrCodeUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/QrCodeUtil.java)
- [PropertiesUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/PropertiesUtil.java)
- [ServerStateUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/ServerStateUtil.java)
- [IpHelper.java](file://sh-web/src/main/java/com/wkclz/web/helper/IpHelper.java)

**Section sources**
- [FileUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/FileUtil.java)
- [CompressUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/CompressUtil.java)
- [NetworkUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/NetworkUtil.java)
- [QrCodeUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/QrCodeUtil.java)
- [PropertiesUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/PropertiesUtil.java)
- [ServerStateUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/ServerStateUtil.java)
- [IpHelper.java](file://sh-web/src/main/java/com/wkclz/web/helper/IpHelper.java)

## Core Components
This section outlines the primary utility components and their responsibilities:
- FileUtil: Provides file and directory operations including reading, writing, copying, moving, and metadata retrieval.
- CompressUtil: Handles compression and decompression for ZIP archives and related formats.
- NetworkUtil: Offers network diagnostics capabilities such as IP detection and port scanning.
- QrCodeUtil: Generates and parses QR codes for barcode functionality.
- PropertiesUtil: Manages configuration properties files for environment-specific settings.
- ServerStateUtil: Monitors server health and collects system metrics for resource usage tracking.

**Section sources**
- [FileUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/FileUtil.java)
- [CompressUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/CompressUtil.java)
- [NetworkUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/NetworkUtil.java)
- [QrCodeUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/QrCodeUtil.java)
- [PropertiesUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/PropertiesUtil.java)
- [ServerStateUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/ServerStateUtil.java)

## Architecture Overview
The utilities are designed as standalone components with minimal coupling. File and compression utilities depend on standard Java I/O APIs. Network utilities integrate with the web helper for IP resolution. Server state utilities rely on OS-level metrics and file system inspection for health checks.

```mermaid
graph TB
Client["Client Applications"] --> FU["FileUtil"]
Client --> CU["CompressUtil"]
Client --> NU["NetworkUtil"]
Client --> QRU["QrCodeUtil"]
Client --> PRU["PropertiesUtil"]
Client --> SSU["ServerStateUtil"]
NU --> IPH["IpHelper"]
SSU --> FU
CU --> FU
```

**Diagram sources**
- [FileUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/FileUtil.java)
- [CompressUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/CompressUtil.java)
- [NetworkUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/NetworkUtil.java)
- [QrCodeUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/QrCodeUtil.java)
- [PropertiesUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/PropertiesUtil.java)
- [ServerStateUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/ServerStateUtil.java)
- [IpHelper.java](file://sh-web/src/main/java/com/wkclz/web/helper/IpHelper.java)

## Detailed Component Analysis

### File Utilities
FileUtil encapsulates common file and directory operations:
- Reading and writing files with configurable encoding and append modes
- Copying and moving files with overwrite controls
- Directory creation, deletion, and traversal
- File metadata retrieval (size, last modified time, permissions)
- Batch operations for efficient processing of multiple files

```mermaid
flowchart TD
Start(["Operation Entry"]) --> ChooseOp{"Choose Operation"}
ChooseOp --> |Read| ReadFile["Read File Content"]
ChooseOp --> |Write| WriteFile["Write File Content"]
ChooseOp --> |Copy| CopyFile["Copy File"]
ChooseOp --> |Move| MoveFile["Move File"]
ChooseOp --> |Directory| DirOps["Directory Operations"]
ReadFile --> End(["Operation Complete"])
WriteFile --> End
CopyFile --> End
MoveFile --> End
DirOps --> End
```

**Diagram sources**
- [FileUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/FileUtil.java)

**Section sources**
- [FileUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/FileUtil.java)

### Compression Utilities
CompressUtil provides compression and decompression capabilities:
- ZIP archive creation and extraction
- GZIP compression and decompression
- Archive validation and integrity checks
- Streaming compression for large files
- Multi-format support for interoperability

```mermaid
flowchart TD
Start(["Compression Entry"]) --> ChooseFormat{"Select Format"}
ChooseFormat --> |ZIP| ZipOps["ZIP Operations"]
ChooseFormat --> |GZIP| GzipOps["GZIP Operations"]
ZipOps --> ProcessZip["Process ZIP Archive"]
GzipOps --> ProcessGzip["Process GZIP Stream"]
ProcessZip --> End(["Compression Complete"])
ProcessGzip --> End
```

**Diagram sources**
- [CompressUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/CompressUtil.java)

**Section sources**
- [CompressUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/CompressUtil.java)

### Network Utilities
NetworkUtil offers network diagnostics and connectivity testing:
- IP address detection and validation
- Port scanning for service availability
- URL handling and normalization
- HTTP operations including GET, POST, and HEAD requests
- DNS resolution and reverse lookup

```mermaid
sequenceDiagram
participant Client as "Client"
participant NetUtil as "NetworkUtil"
participant IpHelper as "IpHelper"
participant Target as "Target Host"
Client->>NetUtil : "Detect IP Address"
NetUtil->>IpHelper : "Resolve Local IP"
IpHelper-->>NetUtil : "Local IP Address"
NetUtil-->>Client : "Detected IP"
Client->>NetUtil : "Scan Ports"
NetUtil->>Target : "Connect to Port"
Target-->>NetUtil : "Connection Result"
NetUtil-->>Client : "Port Scan Results"
```

**Diagram sources**
- [NetworkUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/NetworkUtil.java)
- [IpHelper.java](file://sh-web/src/main/java/com/wkclz/web/helper/IpHelper.java)

**Section sources**
- [NetworkUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/NetworkUtil.java)
- [IpHelper.java](file://sh-web/src/main/java/com/wkclz/web/helper/IpHelper.java)

### QR Code Utilities
QrCodeUtil enables QR code generation and parsing:
- QR code generation from text, URLs, and binary data
- QR code parsing from images and byte arrays
- Configurable error correction levels
- Encoding format selection (UTF-8, ASCII, etc.)
- Image export and customization options

```mermaid
flowchart TD
Start(["QR Operation"]) --> Mode{"Operation Mode"}
Mode --> |Generate| GenQR["Generate QR Code"]
Mode --> |Parse| ParseQR["Parse QR Code"]
GenQR --> Export["Export Image"]
ParseQR --> Decode["Decode Content"]
Export --> End(["Complete"])
Decode --> End
```

**Diagram sources**
- [QrCodeUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/QrCodeUtil.java)

**Section sources**
- [QrCodeUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/QrCodeUtil.java)

### Properties Utilities
PropertiesUtil manages configuration properties files:
- Loading properties from classpath and file system
- Environment-specific property overrides
- Type-safe property retrieval with defaults
- Property encryption and decryption support
- Hot-reload capability for dynamic configuration updates

```mermaid
flowchart TD
Start(["Load Properties"]) --> Source{"Source Type"}
Source --> |Classpath| LoadCP["Load from Classpath"]
Source --> |File System| LoadFS["Load from File System"]
LoadCP --> Merge["Merge with Overrides"]
LoadFS --> Merge
Merge --> EncryptCheck{"Encryption Required?"}
EncryptCheck --> |Yes| Decrypt["Decrypt Values"]
EncryptCheck --> |No| Validate["Validate Types"]
Decrypt --> Validate
Validate --> End(["Properties Ready"])
```

**Diagram sources**
- [PropertiesUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/PropertiesUtil.java)

**Section sources**
- [PropertiesUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/PropertiesUtil.java)

### Server State Utilities
ServerStateUtil monitors system health and resource usage:
- CPU utilization tracking and thresholds
- Memory usage monitoring and garbage collection metrics
- Disk space and inode usage checks
- Process and thread count monitoring
- Health check endpoints and alerting integration

```mermaid
flowchart TD
Start(["Health Check"]) --> Metrics["Collect Metrics"]
Metrics --> CPU["CPU Utilization"]
Metrics --> Memory["Memory Usage"]
Metrics --> Disk["Disk Space"]
CPU --> Thresholds{"Threshold Breach?"}
Memory --> Thresholds
Disk --> Thresholds
Thresholds --> |Yes| Alert["Trigger Alert"]
Thresholds --> |No| Healthy["Healthy Status"]
Alert --> End(["Check Complete"])
Healthy --> End
```

**Diagram sources**
- [ServerStateUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/ServerStateUtil.java)

**Section sources**
- [ServerStateUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/ServerStateUtil.java)

## Dependency Analysis
The utilities maintain loose coupling through clear interfaces and minimal external dependencies. File and compression utilities depend on standard Java I/O APIs. Network utilities integrate with the web helper for IP resolution. Server state utilities rely on OS-level metrics and file system inspection.

```mermaid
graph TB
FU["FileUtil"] --> IO["Java I/O APIs"]
CU["CompressUtil"] --> IO
CU --> FU
NU["NetworkUtil"] --> IPH["IpHelper"]
SSU["ServerStateUtil"] --> FU
SSU --> OS["OS Metrics APIs"]
```

**Diagram sources**
- [FileUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/FileUtil.java)
- [CompressUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/CompressUtil.java)
- [NetworkUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/NetworkUtil.java)
- [IpHelper.java](file://sh-web/src/main/java/com/wkclz/web/helper/IpHelper.java)
- [ServerStateUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/ServerStateUtil.java)

**Section sources**
- [FileUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/FileUtil.java)
- [CompressUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/CompressUtil.java)
- [NetworkUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/NetworkUtil.java)
- [IpHelper.java](file://sh-web/src/main/java/com/wkclz/web/helper/IpHelper.java)
- [ServerStateUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/ServerStateUtil.java)

## Performance Considerations
- Use streaming APIs for large file operations to minimize memory footprint
- Implement batching for bulk operations to reduce I/O overhead
- Leverage compression for network transfers to improve throughput
- Apply connection pooling for HTTP operations to reduce latency
- Monitor resource usage proactively to prevent performance degradation

## Troubleshooting Guide
Common issues and resolutions:
- File permission errors: Verify write permissions and ownership for target directories
- Compression failures: Check available disk space and archive integrity
- Network timeouts: Validate firewall rules and DNS resolution
- QR code parsing errors: Ensure proper lighting and focus during image capture
- Property loading failures: Confirm file paths and encoding compatibility

**Section sources**
- [FileUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/FileUtil.java)
- [CompressUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/CompressUtil.java)
- [NetworkUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/NetworkUtil.java)
- [QrCodeUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/QrCodeUtil.java)
- [PropertiesUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/PropertiesUtil.java)
- [ServerStateUtil.java](file://sh-tool/src/main/java/com/wkclz/tool/utils/ServerStateUtil.java)

## Conclusion
The file system and network utilities provide a robust foundation for file operations, compression, networking, QR code functionality, configuration management, and server monitoring. Their modular design enables easy integration and maintenance while supporting scalable operations across diverse environments.