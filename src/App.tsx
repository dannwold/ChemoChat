/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import React from 'react';
import { Shield, Bluetooth, QrCode, MessageSquare, Settings, Download, Smartphone } from 'lucide-react';

export default function App() {
  return (
    <div className="min-h-screen bg-[#0a0a0a] text-white font-sans selection:bg-emerald-500/30">
      {/* Hero Section */}
      <header className="relative h-[60vh] flex flex-col items-center justify-center overflow-hidden border-b border-white/10">
        <div className="absolute inset-0 opacity-20">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,#10b981,transparent_70%)]" />
        </div>
        
        <div className="relative z-10 text-center px-6">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-medium mb-6 uppercase tracking-wider">
            <Smartphone size={14} />
            Android Project Generated
          </div>
          <h1 className="text-7xl md:text-8xl font-bold tracking-tighter mb-4 bg-clip-text text-transparent bg-gradient-to-b from-white to-white/50">
            ChemoChat
          </h1>
          <p className="text-lg md:text-xl text-zinc-400 max-w-2xl mx-auto font-light leading-relaxed">
            Secure, encrypted peer-to-peer communication over Bluetooth Classic. 
            No internet required. Just privacy.
          </p>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-6xl mx-auto py-20 px-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-20">
          <FeatureCard 
            icon={<Shield className="text-emerald-400" />}
            title="AES-256 Encryption"
            description="All messages, images, and voice clips are encrypted using symmetric AES encryption derived from your shared password."
          />
          <FeatureCard 
            icon={<Bluetooth className="text-blue-400" />}
            title="Bluetooth Classic"
            description="Reliable P2P connection using RFCOMM sockets. Works entirely offline between two Android devices."
          />
          <FeatureCard 
            icon={<QrCode className="text-purple-400" />}
            title="QR Auto-Connect"
            description="Host generates a QR code with MAC address and password. Joiner scans to connect instantly."
          />
        </div>

        {/* Project Structure Section */}
        <section className="bg-zinc-900/50 border border-white/5 rounded-3xl p-8 md:p-12">
          <div className="flex flex-col md:flex-row gap-12">
            <div className="flex-1">
              <h2 className="text-3xl font-semibold mb-6 flex items-center gap-3">
                <MessageSquare className="text-emerald-400" />
                Project Overview
              </h2>
              <p className="text-zinc-400 mb-8 leading-relaxed">
                The full Android Studio project has been generated in the file system. 
                It includes Kotlin source code, Jetpack Compose UI, and custom Bluetooth handling logic.
              </p>
              
              <div className="space-y-4">
                <div className="flex items-center gap-4 p-4 rounded-xl bg-white/5 border border-white/5">
                  <div className="w-10 h-10 rounded-lg bg-emerald-500/20 flex items-center justify-center text-emerald-400">
                    <Settings size={20} />
                  </div>
                  <div>
                    <h4 className="font-medium">Customizable UI</h4>
                    <p className="text-sm text-zinc-500">Local settings for themes and display names.</p>
                  </div>
                </div>
                <div className="flex items-center gap-4 p-4 rounded-xl bg-white/5 border border-white/5">
                  <div className="w-10 h-10 rounded-lg bg-blue-500/20 flex items-center justify-center text-blue-400">
                    <Download size={20} />
                  </div>
                  <div>
                    <h4 className="font-medium">Ready to Build</h4>
                    <p className="text-sm text-zinc-500">Gradle configuration included for Android Studio.</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="flex-1 bg-black/40 rounded-2xl p-6 border border-white/5 font-mono text-sm text-zinc-500 overflow-hidden">
              <div className="flex items-center gap-2 mb-4 text-zinc-400">
                <div className="w-3 h-3 rounded-full bg-red-500/50" />
                <div className="w-3 h-3 rounded-full bg-yellow-500/50" />
                <div className="w-3 h-3 rounded-full bg-green-500/50" />
                <span className="ml-2 text-xs opacity-50">Android Project Structure</span>
              </div>
              <ul className="space-y-2">
                <li className="text-emerald-400">app/src/main/java/com/example/chemochat/</li>
                <li className="pl-4">├── MainActivity.kt</li>
                <li className="pl-4">├── BluetoothService.kt</li>
                <li className="pl-4">├── EncryptionUtils.kt</li>
                <li className="pl-4">└── ChatViewModel.kt</li>
                <li className="text-zinc-400">app/src/main/res/layout/</li>
                <li className="text-zinc-400">build.gradle</li>
                <li className="text-zinc-400">AndroidManifest.xml</li>
              </ul>
            </div>
          </div>
        </section>

        <footer className="mt-20 text-center text-zinc-600 text-sm">
          <p>ChemoChat &copy; 2026 • Secure P2P Communication</p>
          <p className="mt-2">Export this project to ZIP to open in Android Studio.</p>
        </footer>
      </main>
    </div>
  );
}

function FeatureCard({ icon, title, description }: { icon: React.ReactNode, title: string, description: string }) {
  return (
    <div className="p-8 rounded-3xl bg-zinc-900/30 border border-white/5 hover:border-emerald-500/20 transition-colors group">
      <div className="mb-6 transform group-hover:scale-110 transition-transform duration-300">
        {icon}
      </div>
      <h3 className="text-xl font-medium mb-3">{title}</h3>
      <p className="text-zinc-500 leading-relaxed font-light">{description}</p>
    </div>
  );
}
