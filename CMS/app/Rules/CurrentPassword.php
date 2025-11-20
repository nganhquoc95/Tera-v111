<?php

namespace App\Rules;

use Closure;
use Illuminate\Contracts\Validation\ValidationRule;
use Illuminate\Support\Facades\Auth;
use App\Models\Account;

class CurrentPassword implements ValidationRule
{
    /**
     * Run the validation rule.
     */
    public function validate(string $attribute, mixed $value, Closure $fail): void
    {
        /** @var Account $user */
        $user = Auth::user();

        if (!$user || !$user->verifyPassword($value)) {
            $fail('The provided password is incorrect.');
        }
    }
}
