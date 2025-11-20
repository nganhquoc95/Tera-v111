<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
            border: 1px solid #ddd;
            border-radius: 8px;
        }
        .header {
            background-color: #f5f5f5;
            padding: 20px;
            border-radius: 8px 8px 0 0;
            margin: -20px -20px 20px -20px;
        }
        .header h1 {
            margin: 0;
            color: #333;
        }
        .content {
            margin: 20px 0;
        }
        .button {
            display: inline-block;
            background-color: #007bff;
            color: white;
            padding: 12px 24px;
            text-decoration: none;
            border-radius: 4px;
            margin: 20px 0;
        }
        .button:hover {
            background-color: #0056b3;
        }
        .footer {
            margin-top: 30px;
            padding-top: 20px;
            border-top: 1px solid #ddd;
            font-size: 12px;
            color: #666;
        }
        .code {
            background-color: #f5f5f5;
            padding: 10px;
            border-radius: 4px;
            font-family: monospace;
            word-break: break-all;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Password Reset Request</h1>
        </div>

        <div class="content">
            <p>Hello {{ $accountName }},</p>

            <p>We received a request to reset your account password. If you didn't make this request, you can safely ignore this email.</p>

            <p>To reset your password, click the button below:</p>

            <a href="{{ $resetUrl }}" class="button">Reset Password</a>

            <p>Or copy and paste this link in your browser:</p>
            <p><code class="code">{{ $resetUrl }}</code></p>

            <p><strong>Reset Key:</strong> <code class="code">{{ $confirmKey }}</code></p>

            <p>This link will expire in 24 hours.</p>

            <p>If you have any questions, please contact us.</p>
        </div>

        <div class="footer">
            <p>© {{ date('Y') }} All rights reserved.</p>
            <p>This is an automated email, please do not reply directly.</p>
        </div>
    </div>
</body>
</html>
