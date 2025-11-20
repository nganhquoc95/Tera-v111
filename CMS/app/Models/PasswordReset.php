<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class PasswordReset extends Model
{
    protected $table = 'pwreset';
    protected $primaryKey = 'id';
    public $timestamps = false;

    protected $fillable = [
        'id',
        'name',
        'email',
        'confirmkey',
        'status',
        'timestamp',
    ];

    protected $casts = [
        'id' => 'integer',
        'status' => 'integer',
    ];

    /**
     * Check if reset key is valid and not expired (24 hours)
     */
    public function isValid(): bool
    {
        if ($this->status === 1) {
            return false; // Already used
        }

        $expirationTime = (int) $this->timestamp + (24 * 60 * 60); // 24 hours
        return now()->getTimestamp() < $expirationTime;
    }

    /**
     * Get associated account
     */
    public function account()
    {
        return $this->belongsTo(Account::class, 'name', 'name');
    }
}
