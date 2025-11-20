<?php

namespace App\Notifications;

use App\Mail\PasswordResetMail;
use App\Models\Account;
use App\Models\PasswordReset;
use Illuminate\Notifications\Notification;

class SendPasswordResetNotification extends Notification
{
    public function __construct(
        public PasswordReset $reset,
        public Account $account,
    ) {}

    /**
     * Get the notification's delivery channels.
     *
     * @return array<int, string>
     */
    public function via(object $notifiable): array
    {
        return ['mail'];
    }

    /**
     * Get the mail representation of the notification.
     */
    public function toMail(object $notifiable): PasswordResetMail
    {
        return new PasswordResetMail($this->reset, $this->account);
    }
}
