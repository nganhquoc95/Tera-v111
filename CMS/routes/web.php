<?php

use App\Http\Controllers\AccountController;
use App\Http\Controllers\Auth\LoginController;
use App\Http\Controllers\Auth\LogoutController;
use App\Http\Controllers\Auth\PasswordResetController;
use App\Http\Controllers\Auth\RegisterController;
use Illuminate\Support\Facades\Route;
use Inertia\Inertia;

Route::get('/', function () {
    return Inertia::render('welcome');
})->name('home');

Route::get('login', [LoginController::class, 'show'])->name('login');
Route::post('login', [LoginController::class, 'authenticate'])->name('login.authenticate');
Route::post('logout', [LogoutController::class, 'store'])->name('logout');

Route::get('register', [RegisterController::class, 'show'])->name('register');
Route::post('register', [RegisterController::class, 'register'])->name('register.create');

// Password Reset Routes
Route::get('forgot-password', [PasswordResetController::class, 'showRequestForm'])->name('password.request');
Route::post('forgot-password', [PasswordResetController::class, 'sendResetLink'])->name('password.email');
Route::get('reset-password', [PasswordResetController::class, 'showResetForm'])->name('password.reset.form');
Route::post('reset-password', [PasswordResetController::class, 'resetPassword'])->name('password.reset');

Route::middleware('auth')->group(function () {
    Route::get('dashboard', function () {
        return Inertia::render('dashboard');
    })->name('dashboard');

    // Account Management Routes
    Route::resource('accounts', AccountController::class);
    
    // Additional API endpoints
    Route::prefix('accounts')->group(function () {
        Route::get('search', [AccountController::class, 'search']);
        Route::post('bulk-delete', [AccountController::class, 'bulkDelete']);
        Route::patch('{account}/ban', [AccountController::class, 'toggleBan']);
        Route::patch('{account}/tempban', [AccountController::class, 'setTempBan']);
        Route::get('stats', [AccountController::class, 'stats']);
    });
});

require __DIR__.'/settings.php';
