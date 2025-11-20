<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        if (Schema::hasTable('accounts')) {
            return;
        }

        Schema::create('accounts', function (Blueprint $table) {
            $table->integer('id')->primary();
            $table->string('name', 13)->unique();
            $table->string('password', 128);
            $table->string('salt', 32)->nullable();
            $table->string('2ndpassword', 134)->nullable();
            $table->string('salt2', 32)->nullable();
            $table->tinyInteger('loggedin')->unsigned()->default(0);
            $table->timestamp('lastlogin')->nullable();
            $table->timestamp('createdat')->useCurrent();
            $table->dateTime('birthday')->useCurrent();
            $table->tinyInteger('banned')->default(0);
            $table->text('banreason')->nullable();
            $table->tinyInteger('gm')->default(0);
            $table->text('email')->nullable();
            $table->text('macs')->nullable();
            $table->timestamp('tempban')->nullable();
            $table->unsignedTinyInteger('greason')->nullable();
            $table->integer('NxPrepaid')->default(0);
            $table->integer('NxCredit')->default(0);
            $table->integer('mPoints')->default(0);
            $table->unsignedTinyInteger('gender')->default(0);
            $table->string('SessionIP', 64)->nullable();
            $table->integer('points')->default(0);
            $table->integer('vpoints')->default(0);
            $table->integer('monthvotes')->default(0);
            $table->integer('totalvotes')->default(0);
            $table->integer('lastvote')->default(0);
            $table->integer('lastvote2')->default(0);
            $table->timestamp('lastlogon')->nullable();
            $table->string('lastvoteip', 64)->nullable();
            $table->tinyInteger('PicEnabled')->default(0);
            
            $table->index('name');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('accounts');
    }
};
