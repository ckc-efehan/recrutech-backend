# Recrutech Authentication Service - Database Schema Documentation

## 📋 Overview

This directory contains the organized Liquibase changelog files for the Recrutech Authentication Service database schema. The schema has been restructured from a monolithic approach to a modular, maintainable structure.

## 🏗️ Architecture

### Directory Structure
```
db.changelog/
├── liquibase-changelog.xml          # Master changelog file
├── v1.0/                           # Version 1.0 schema files
│   ├── 01-create-users-table.xml      # Core authentication
│   ├── 02-create-companies-table.xml  # Company management
│   ├── 03-create-hr-employees-table.xml # HR management
│   └── 04-create-applicants-table.xml # Applicant management
└── README.md                       # This documentation
```

### Schema Modules

#### 1. Core Authentication Module (`01-create-users-table.xml`)
- **Purpose**: Foundation authentication and security infrastructure
- **Dependencies**: None (base table)
- **Features**:
  - User authentication (email, password, role)
  - Email verification system
  - Account security (failed attempts, lockout)
  - Password management (reset, expiry)
  - Two-factor authentication support
  - Session and token management
  - Login tracking and audit fields
  - Advanced performance indexes

#### 2. Company Management Module (`02-create-companies-table.xml`)
- **Purpose**: Business entity management
- **Dependencies**: users table (admin_user_id foreign key)
- **Features**:
  - Company information (name, location, contact details)
  - Business email and telephone (unique constraints)
  - Admin user relationship
  - Company verification system
  - Enhanced indexing for search operations

#### 3. HR Management Module (`03-create-hr-employees-table.xml`)
- **Purpose**: HR employee lifecycle management
- **Dependencies**: users and companies tables
- **Features**:
  - User-company relationships
  - HR-specific information (department, position, hire date)
  - Employee identification and status tracking
  - Unique constraints for data integrity
  - Composite indexes for complex queries

#### 4. Applicant Management Module (`04-create-applicants-table.xml`)
- **Purpose**: Job applicant profile management
- **Dependencies**: users table
- **Features**:
  - Personal and professional information
  - Contact details and location information
  - Profile completion tracking
  - Professional links (LinkedIn, resume)
  - Data validation with check constraints

## 🔄 Dependency Order

The changelog files are executed in the following order to ensure referential integrity:

1. **users** → Base table with no dependencies
2. **companies** → Depends on users (admin_user_id)
3. **hr_employees** → Depends on users and companies
4. **applicants** → Depends on users

## ✨ Key Improvements

### From Monolithic to Modular
- **Before**: Single 187-line file with all tables
- **After**: 4 focused modules with clear separation of concerns

### Enhanced Documentation
- Comprehensive comments in each changelog
- Clear dependency documentation
- Business context for each table and field

### Improved Performance
- Additional performance indexes beyond the original schema
- Composite indexes for complex query patterns
- Strategic indexing for common operations

### Data Integrity
- Enhanced foreign key constraints with CASCADE/RESTRICT options
- Unique constraints for business rules
- Check constraints for data validation (phone numbers, URLs)

### Maintainability
- Modular structure for easier maintenance
- Clear versioning strategy (v1.0, v1.1, v2.0)
- Separation of concerns by business domain

## 🚀 Usage

### Running Migrations
The master changelog file (`liquibase-changelog.xml`) orchestrates all migrations:

```bash
# Run all migrations
mvn liquibase:update

# Validate changelog syntax
mvn liquibase:validate

# Generate SQL preview
mvn liquibase:updateSQL
```

### Adding New Changes
For future schema changes, follow this pattern:

1. **Minor Changes (v1.1)**: Add new changelog files in `v1.0/` directory
2. **Major Changes (v2.0)**: Create new `v2.0/` directory
3. **Update Master**: Add includes to `liquibase-changelog.xml`

### Example for v1.1 Changes
```xml
<!-- In liquibase-changelog.xml -->
<include file="db.changelog/v1.0/05-add-user-preferences-table.xml"/>
```

## 📊 Schema Statistics

| Module | Tables | Indexes | Constraints | Lines of Code |
|--------|--------|---------|-------------|---------------|
| Users | 1 | 5 | 1 PK | 124 |
| Companies | 1 | 5 | 1 PK + 1 FK | 119 |
| HR Employees | 1 | 6 | 1 PK + 2 FK + 2 UK | 147 |
| Applicants | 1 | 6 | 1 PK + 1 FK + 3 UK + 3 CK | 174 |
| **Total** | **4** | **22** | **15** | **564** |

## 🔧 Maintenance Guidelines

### Best Practices
1. **Never modify existing changesets** - Always add new ones
2. **Use descriptive changeset IDs** - Include version and purpose
3. **Document dependencies** - Clearly state table relationships
4. **Test thoroughly** - Validate on clean database before deployment
5. **Version appropriately** - Follow semantic versioning

### Troubleshooting
- **Validation errors**: Check XML syntax and schema references
- **Dependency issues**: Verify include file paths and execution order
- **Performance issues**: Review index usage and query patterns

## 📝 Version History

### v1.0.0 (Current)
- Initial modular schema creation
- Complete authentication infrastructure
- Company and employee management
- Applicant profile system
- Enhanced indexing and constraints

### Future Versions
- v1.1: Additional features and optimizations
- v1.2: Extended user preferences and settings
- v2.0: Major architectural changes

---

**Author**: Efehan Cekic [efehan.cekic@student.htw-berlin.de]  
**Last Updated**: 2025-08-24  
**Version**: 1.0.0