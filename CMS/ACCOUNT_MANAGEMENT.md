# Account Management System

A comprehensive web-based account management system built with Laravel and React, featuring SHA-1 password encryption. Designed to work with existing MapleStory v111 account database structure.

## Features

- **Create Accounts**: Create new accounts with account name, email, gender, and password
- **View Accounts**: Browse all accounts with detailed information including currency and points
- **Edit Accounts**: Update account details and passwords
- **Delete Accounts**: Remove accounts from the system
- **Ban/Unban Accounts**: Toggle account ban status
- **Bulk Operations**: Delete multiple accounts at once
- **Search**: Search accounts by name or email
- **Status Management**: View account status (Active, Banned, Online)
- **GM Accounts**: Identify GM (Game Master) accounts
- **Currency Tracking**: View NX Prepaid, NX Credit, Points, and Vote Points
- **Secure Passwords**: All passwords are encrypted using SHA-1 algorithm

## Database Schema

The system uses the existing `accounts` table with the following structure:

| Column | Type | Notes |
|--------|------|-------|
| id | INT | Primary Key |
| name | VARCHAR(13) | Unique account name |
| password | VARCHAR(128) | SHA-1 encrypted password |
| salt | VARCHAR(32) | Optional salt for password |
| 2ndpassword | VARCHAR(134) | Secondary password (nullable) |
| salt2 | VARCHAR(32) | Secondary password salt (nullable) |
| loggedin | TINYINT | Login status (0/1) |
| lastlogin | TIMESTAMP | Last login time |
| createdat | TIMESTAMP | Account creation time |
| birthday | DATETIME | Account birthday |
| banned | TINYINT | Ban status (0=active, 1=banned) |
| banreason | TEXT | Reason for ban |
| gm | TINYINT | GM status (0/1) |
| email | TEXT | Email address |
| macs | TEXT | MAC addresses |
| tempban | TIMESTAMP | Temporary ban expiration |
| greason | TINYINT | GM reason code |
| NxPrepaid | INT | NX Prepaid currency |
| NxCredit | INT | NX Credit currency |
| mPoints | INT | Maple Points |
| gender | TINYINT | Gender (0=Male, 1=Female) |
| SessionIP | VARCHAR(64) | Current session IP |
| points | INT | Game points |
| vpoints | INT | Vote points |
| monthvotes | INT | Votes this month |
| totalvotes | INT | Total votes |
| lastvote | INT | Last vote timestamp |
| lastvote2 | INT | Secondary vote timestamp |
| lastlogon | TIMESTAMP | Last logon time |
| lastvoteip | VARCHAR(64) | IP of last vote |
| PicEnabled | TINYINT | Picture enabled (0/1) |

## Installation

### 1. Run Migrations

```bash
php artisan migrate
```

**Note**: The migration is configured to work with the existing `accounts` table structure. It will create the table with all the proper fields if it doesn't exist.

### 2. File Structure

```
app/
  Models/
    Account.php              # Account model with SHA-1 hashing
  Http/
    Controllers/
      AccountController.php  # Account management controller
    Requests/
      StoreAccountRequest.php    # Validation for creating accounts
      UpdateAccountRequest.php   # Validation for updating accounts

resources/js/
  pages/
    accounts/
      index.tsx              # List all accounts
      create.tsx             # Create new account form
      show.tsx               # View account details
      edit.tsx               # Edit account form
  lib/
    dateUtils.ts             # Date formatting utilities

routes/
  web.php                     # Account management routes
```

## API Endpoints

### Account CRUD Operations

- `GET /accounts` - List all accounts
- `GET /accounts/create` - Show create form
- `POST /accounts` - Store new account
- `GET /accounts/{id}` - View account details
- `GET /accounts/{id}/edit` - Show edit form
- `PATCH /accounts/{id}` - Update account
- `DELETE /accounts/{id}` - Delete account

### Additional Endpoints

- `GET /accounts/search?query=...` - Search accounts by name/email
- `POST /accounts/bulk-delete` - Delete multiple accounts
- `PATCH /accounts/{id}/ban` - Toggle account ban status
- `PATCH /accounts/{id}/tempban` - Set temporary ban
- `GET /accounts/stats` - Get account statistics

## Usage Examples

### Create an Account

```php
POST /accounts
Content-Type: application/json

{
  "name": "PlayerName",
  "email": "player@example.com",
  "password": "securepassword",
  "password_confirmation": "securepassword",
  "gender": "0"
}
```

### Update Account

```php
PATCH /accounts/1
Content-Type: application/json

{
  "name": "NewPlayerName",
  "email": "newemail@example.com",
  "password": "newpassword",
  "password_confirmation": "newpassword",
  "gender": "0"
}
```

### Ban Account

```php
PATCH /accounts/1/ban
Content-Type: application/json

{
  "banned": 1,
  "banreason": "Unauthorized access"
}
```

### Set Temporary Ban

```php
PATCH /accounts/1/tempban
Content-Type: application/json

{
  "tempban": "2025-11-30 23:59:59"
}
```

### Delete Account

```php
DELETE /accounts/1
```

### Bulk Delete

```php
POST /accounts/bulk-delete
Content-Type: application/json

{
  "ids": [1, 2, 3]
}
```

### Get Statistics

```php
GET /accounts/stats
```

Response:
```json
{
  "total": 150,
  "banned": 5,
  "gm": 3,
  "loggedIn": 12
}
```

## Password Encryption

Passwords are automatically encrypted using SHA-1 when:
1. Creating a new account
2. Updating an account's password

### SHA-1 Implementation

The `Account` model includes:
- `hashPassword(string $password): string` - Static method to hash password
- `verifyPassword(string $password): bool` - Verify password against hash
- Automatic hashing via `setPasswordAttribute()` mutator

```php
// Hash a password
$hash = Account::hashPassword('mypassword');
// Result: 356a192b7913b04c54574d18c28d46e6395428ab

// Verify password
if ($account->verifyPassword('mypassword')) {
    // Password is correct
}
```

## Validation Rules

### Create Account
- `name`: Required, 3-13 characters, unique
- `email`: Optional, valid email format, unique
- `password`: Required, minimum 4 characters, must be confirmed
- `gender`: Optional, must be 0 or 1

### Update Account
- `name`: Optional, 3-13 characters, unique (except current account)
- `email`: Optional, valid email format, unique (except current account)
- `password`: Optional, minimum 4 characters, must be confirmed if provided
- `gender`: Optional, must be 0 or 1
- `banreason`: Optional, string
- `banned`: Optional, boolean

## Frontend Components

### Account List (index.tsx)
- Displays all accounts in a table
- Search functionality by name or email
- Bulk delete with checkbox selection
- Status badges showing Active/Banned
- GM indicator
- Quick action links (View, Edit)
- Account creation and last login timestamps

### Create Account (create.tsx)
- Form with name, email, gender, and password fields
- Password confirmation field
- Form validation with error messages
- Gender selection (Male/Female)

### View Account (show.tsx)
- Display complete account details
- Status indicator (Active/Banned) with toggle button
- Online status indicator
- GM badge display
- Currency and points information
- Edit and Delete buttons
- Account timestamps and login history

### Edit Account (edit.tsx)
- Pre-filled form with current account data
- Optional password change
- Password confirmation field
- Gender selection
- Form validation with error messages

## Gender Values

- `0` - Male
- `1` - Female

## Ban Status

The system uses a simple boolean value for ban status:
- `0` - Active (not banned)
- `1` - Banned

For temporary bans, use the `tempban` field which stores a timestamp indicating when the ban expires.

## Security Notes

⚠️ **Important**: While SHA-1 is implemented as requested for existing database compatibility:
- SHA-1 is considered cryptographically weak by modern standards
- This implementation is for compatibility with existing MapleStory databases
- For new projects or security-critical systems, consider using bcrypt or Argon2
- Always use HTTPS in production
- Implement additional security measures (rate limiting, 2FA, etc.)

## Dependencies

- Laravel 12.0+
- React with Inertia.js
- TypeScript
- Tailwind CSS

## Contributing

To extend the account management system:

1. Add new fields to the migration if needed
2. Update the `Account` model's `$fillable` array
3. Add validation rules to `StoreAccountRequest` and `UpdateAccountRequest`
4. Update controller methods as needed
5. Update React components to display new fields

## Example: Adding a New Field

1. **Update the model** (`app/Models/Account.php`):
   ```php
   protected $fillable = [
       // ... existing fields
       'newfield',
   ];
   ```

2. **Update form requests**:
   ```php
   'newfield' => 'sometimes|string|max:255',
   ```

3. **Update React components** to include the new field in forms

## License

This account management system is part of the TeraServer project.
