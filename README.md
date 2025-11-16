# Tera
v111.1 Server Emulator

Requires Java 16+

Passion project from existing source (Lidium)

## Development Setup

### Hot-Reload (HotSwap) Development

The server supports hot-reloading of Java code changes without restarting during development.

#### Quick Start
1. Run `.\dev.bat` - starts server in debug mode with instructions
2. In VS Code: Run → Start Debugging → "Attach to Server (HotSwap)"
3. Make code changes in `src/` folder
4. Run `.\build.bat` to compile changes
5. In debugger: Click restart button (F5) to hotswap classes

#### Manual Setup
1. Start server in debug mode: `.\launch.bat debug`
   - Runs `server.Start` class directly with `build/` directory in classpath
   - Enables JDWP debugging on port 5005
2. Attach debugger to port 5005
3. Make changes and rebuild
4. Use debugger's hotswap feature

#### VS Code Setup
- Debug configuration "Attach to Server (HotSwap)" automatically builds before attaching
- Build task defined in `.vscode/tasks.json`
- Hotswap works for method body changes, adding/removing methods requires restart

#### Limitations
- Can only hotswap method implementations
- Adding/removing methods/fields/classes requires restart
- Static field changes may not apply correctly