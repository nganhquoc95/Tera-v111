<?php

namespace Tests\Feature\Auth;

use App\Models\Account;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\RateLimiter;
use Tests\TestCase;

class AuthenticationTest extends TestCase
{
    use RefreshDatabase;

    public function test_login_screen_can_be_rendered()
    {
        $response = $this->get(route('login'));

        $response->assertStatus(200);
    }

    public function test_users_can_authenticate_using_the_login_screen()
    {
        $user = Account::factory()->withoutTwoFactor()->create();

        $response = $this->post(route('login.authenticate'), [
            'name' => $user->name,
            'password' => 'password',
        ]);

        $this->assertAuthenticated();
        $response->assertRedirect(route('dashboard', absolute: false));
    }

    public function test_users_can_not_authenticate_with_invalid_password()
    {
        $user = Account::factory()->create();

        $this->post(route('login.authenticate'), [
            'name' => $user->name,
            'password' => 'wrong-password',
        ]);

        $this->assertGuest();
    }

    public function test_users_can_logout()
    {
        $user = Account::factory()->create();

        $response = $this->actingAs($user, 'web')->post(route('logout'));

        $this->assertGuest();
        $response->assertRedirect(route('home'));
    }

    public function test_users_are_rate_limited()
    {
        $user = Account::factory()->create();

        RateLimiter::increment(md5('login'.implode('|', [$user->name, '127.0.0.1'])), amount: 5);

        $response = $this->post(route('login.authenticate'), [
            'name' => $user->name,
            'password' => 'wrong-password',
        ]);

        $response->assertTooManyRequests();
    }
}
