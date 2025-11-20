<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

class StoreAccountRequest extends FormRequest
{
    /**
     * Determine if the user is authorized to make this request.
     */
    public function authorize(): bool
    {
        return true;
    }

    /**
     * Get the validation rules that apply to the request.
     */
    public function rules(): array
    {
        return [
            'name' => 'required|string|min:3|max:13|unique:accounts',
            'email' => 'sometimes|email|unique:accounts',
            'password' => 'required|string|min:4|confirmed',
            'banned' => 'sometimes|boolean',
            'gm' => 'sometimes|boolean',
            'gender' => 'sometimes|integer|in:0,1',
            'birthday' => 'sometimes|date',
        ];
    }

    /**
     * Get custom messages for validator errors.
     */
    public function messages(): array
    {
        return [
            'name.required' => 'Account name is required',
            'name.min' => 'Account name must be at least 3 characters',
            'name.max' => 'Account name cannot exceed 13 characters',
            'name.unique' => 'Account name already exists',
            'email.email' => 'Email must be valid',
            'email.unique' => 'Email already exists',
            'password.required' => 'Password is required',
            'password.min' => 'Password must be at least 4 characters',
            'password.confirmed' => 'Passwords do not match',
            'gender.in' => 'Invalid gender value',
        ];
    }
}
