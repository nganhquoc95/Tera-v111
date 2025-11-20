<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;

class Account extends Authenticatable
{
    use HasFactory, Notifiable;

    protected $table = 'accounts';
    protected $primaryKey = 'id';
    public $timestamps = false;

    protected $fillable = [
        'id',
        'name',
        'password',
        'salt',
        '2ndpassword',
        'salt2',
        'loggedin',
        'lastlogin',
        'createdat',
        'birthday',
        'banned',
        'banreason',
        'gm',
        'email',
        'macs',
        'tempban',
        'greason',
        'NxPrepaid',
        'NxCredit',
        'mPoints',
        'gender',
        'SessionIP',
        'points',
        'vpoints',
        'monthvotes',
        'totalvotes',
        'lastvote',
        'lastvote2',
        'lastlogon',
        'lastvoteip',
        'PicEnabled',
    ];

    protected $hidden = [
        'password',
        'salt',
        '2ndpassword',
        'salt2',
        'remember_token',
        'two_factor_secret',
        'two_factor_recovery_codes',
    ];

    protected function casts(): array
    {
        return [
            'createdat' => 'datetime',
            'lastlogin' => 'datetime',
            'birthday' => 'datetime',
            'tempban' => 'datetime',
            'lastlogon' => 'datetime',
            'banned' => 'boolean',
            'gm' => 'boolean',
            'PicEnabled' => 'boolean',
        ];
    }

    /**
     * Get the name of the unique identifier for the user.
     */
    public function getAuthIdentifierName(): string
    {
        return 'id';
    }

    /**
     * Get the unique identifier for the user.
     */
    public function getAuthIdentifier()
    {
        return $this->{$this->getAuthIdentifierName()};
    }

    /**
     * Get the password for the user.
     */
    public function getAuthPassword(): string
    {
        return $this->password;
    }

    /**
     * Get the token value for the "remember me" functionality.
     */
    public function getRememberToken(): ?string
    {
        return $this->remember_token ?? null;
    }

    /**
     * Set the token value for the "remember me" functionality.
     */
    public function setRememberToken($value): void
    {
        $this->remember_token = $value;
    }

    /**
     * Get the column name for the "remember me" token.
     */
    public function getRememberTokenName(): string
    {
        return 'remember_token';
    }

    /**
     * Hash password using bcrypt algorithm
     * New passwords are hashed with bcrypt for better security
     */
    public static function hashPasswordBcrypt(string $password): string
    {
        return bcrypt($password);
    }

    /**
     * Hash password using SHA-512 algorithm with salt
     * Legacy method: password + salt (for existing passwords)
     */
    public static function hashPasswordSha512(string $password, string $salt = ''): string
    {
        return hash('sha512', $password . $salt);
    }

    /**
     * Verify password against either bcrypt or SHA-512 hash
     */
    public function verifyPassword(string $password): bool
    {
        // Try bcrypt first (new passwords)
        if (password_verify($password, $this->password)) {
            return true;
        }

        // Fall back to SHA-512 (legacy passwords)
        return $this->password === self::hashPasswordSha512($password, $this->salt);
    }

    /**
     * Set the password attribute and hash it with bcrypt
     */
    public function setPasswordAttribute(string $value): void
    {
        // Use bcrypt for new passwords
        $this->attributes['password'] = self::hashPasswordBcrypt($value);
    }

    /**
     * Get account status based on ban status
     */
    public function getStatusAttribute(): string
    {
        if ($this->banned) {
            return 'banned';
        }
        return 'active';
    }

    /**
     * Check if account is banned
     */
    public function isBanned(): bool
    {
        return (bool) $this->banned;
    }

    /**
     * Check if account is temporary banned
     */
    public function isTempBanned(): bool
    {
        if (!$this->tempban) {
            return false;
        }
        
        $tempbanTime = strtotime($this->tempban);
        $now = time();
        
        return $now < $tempbanTime;
    }
}

