<?php

namespace App\Mail;

use App\Models\Account;
use App\Models\PasswordReset;
use Illuminate\Bus\Queueable;
use Illuminate\Mail\Mailable;
use Illuminate\Mail\Mailables\Address;
use Illuminate\Mail\Mailables\Content;
use Illuminate\Mail\Mailables\Envelope;
use Illuminate\Queue\SerializesModels;

class PasswordResetMail extends Mailable
{
    use Queueable, SerializesModels;

    public function __construct(
        public PasswordReset $reset,
        public Account $account,
    ) {}

    /**
     * Get the message envelope.
     */
    public function envelope(): Envelope
    {
        return new Envelope(
            from: new Address(config('mail.from.address'), config('mail.from.name')),
            subject: 'Reset Your Password',
        );
    }

    /**
     * Get the message content definition.
     */
    public function content(): Content
    {
        $resetUrl = route('password.reset.form', ['key' => $this->reset->confirmkey]);

        return new Content(
            view: 'emails.password-reset',
            with: [
                'accountName' => $this->account->name,
                'resetUrl' => $resetUrl,
                'confirmKey' => $this->reset->confirmkey,
            ],
        );
    }

    /**
     * Get the attachments for the message.
     *
     * @return array<int, \Illuminate\Mail\Mailables\Attachment>
     */
    public function attachments(): array
    {
        return [];
    }
}
