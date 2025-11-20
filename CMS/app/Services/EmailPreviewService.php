<?php

namespace App\Services;

use App\Models\Account;
use App\Models\PasswordReset;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Str;

class EmailPreviewService
{
    /**
     * Generate HTML preview of password reset email and open in browser (Development only)
     */
    public function generateAndOpenPasswordResetPreview(PasswordReset $reset, Account $account): void
    {
        if (!app()->environment('local', 'development')) {
            return;
        }

        try {
            $resetUrl = route('password.reset.form', ['key' => $reset->confirmkey]);

            $html = view('emails.password-reset', [
                'accountName' => $account->name,
                'resetUrl' => $resetUrl,
                'confirmKey' => $reset->confirmkey,
            ])->render();

            $filePath = $this->saveHtmlPreview($html);
            $fileUrl = $this->getFileUrl($filePath);

            $this->openInBrowser($fileUrl);

            Log::info("Email preview generated at: {$filePath}");
        } catch (\Exception $e) {
            Log::error("Failed to generate email preview: " . $e->getMessage());
        }
    }

    /**
     * Save HTML preview to tmp directory
     */
    private function saveHtmlPreview(string $html): string
    {
        $filename = 'password-reset-' . Str::random(8) . '.html';
        $filePath = base_path('tmp/' . $filename);

        // Ensure tmp directory exists
        if (!is_dir(base_path('tmp'))) {
            mkdir(base_path('tmp'), 0755, true);
        }

        file_put_contents($filePath, $html);

        return $filePath;
    }

    /**
     * Convert file path to file URL
     */
    private function getFileUrl(string $filePath): string
    {
        return 'file:///' . str_replace('\\\\', '/', $filePath);
    }

    /**
     * Open URL in default browser based on operating system
     */
    private function openInBrowser(string $url): void
    {
        $os = strtolower(PHP_OS_FAMILY);

        if ($os === 'windows') {
            // Windows
            exec('start ' . escapeshellarg($url));
        } elseif ($os === 'darwin') {
            // macOS
            exec('open ' . escapeshellarg($url));
        } else {
            // Linux and others
            exec('xdg-open ' . escapeshellarg($url) . ' > /dev/null 2>&1 &');
        }
    }
}
