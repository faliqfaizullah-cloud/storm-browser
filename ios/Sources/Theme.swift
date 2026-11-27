import SwiftUI
import UIKit

extension Color {
    init(hex: UInt32) {
        self.init(.sRGB, red: Double((hex >> 16) & 255) / 255, green: Double((hex >> 8) & 255) / 255, blue: Double(hex & 255) / 255, opacity: 1)
    }
}

/// Blend a colour 40% towards white (used by the Minimal theme).
func softHex(_ h: UInt32) -> UInt32 {
    func f(_ v: UInt32) -> UInt32 { UInt32(Double(v) * 0.6 + 255 * 0.4) }
    return (f((h >> 16) & 255) << 16) | (f((h >> 8) & 255) << 8) | f(h & 255)
}

func n2(_ n: Int) -> String { String(format: "%02d", n) }

struct Palette {
    static let accents: [UInt32] = [0xFF5500, 0x4A4DFF, 0xC8F23C, 0xFF4FA3, 0x8E6BFF, 0xFFC928]
    let theme: Int          // 0 light, 1 OLED, 2 minimal
    let accentIndex: Int

    var mini: Bool { theme == 2 }
    var paper: Color { theme == 1 ? Color.black : (theme == 2 ? Color.white : Color(hex: 0xF0EEE9)) }
    var ink: Color { theme == 1 ? Color(hex: 0xF2F2F2) : (theme == 2 ? Color(hex: 0x1C1C1E) : Color(hex: 0x111111)) }
    var line: CGFloat { theme == 2 ? 0.7 : 1.6 }
    var acc: Color {
        let h = Palette.accents[accentIndex % Palette.accents.count]
        return Color(hex: mini ? softHex(h) : h)
    }
    var onAcc: Color { (!mini && (accentIndex == 1 || accentIndex == 4)) ? Color.white : Color(hex: 0x111111) }

    func D(_ s: CGFloat) -> Font { mini ? Font.system(size: s, weight: .light) : Font.custom("AvenirNextCondensed-Heavy", size: s) }
    func L(_ s: CGFloat) -> Font { mini ? Font.system(size: s, weight: .medium) : Font.custom("AvenirNextCondensed-DemiBold", size: s) }
}

/// Big display text (heavy condensed, upper-case — or light sentence-case in the Minimal theme).
struct DText: View {
    @ObservedObject var store = Store.shared
    let s: String
    let size: CGFloat
    var color: Color? = nil
    var body: some View {
        let p = store.palette
        Text(p.mini ? s : s.uppercased())
            .font(p.D(size))
            .foregroundColor(color ?? p.ink)
            .lineSpacing(p.mini ? 0 : -size * 0.12)
    }
}

/// Small label text.
struct LText: View {
    @ObservedObject var store = Store.shared
    let s: String
    let size: CGFloat
    var color: Color? = nil
    var body: some View {
        let p = store.palette
        Text(p.mini ? s : s.uppercased())
            .font(p.L(size))
            .tracking(p.mini ? 0.2 : 0.6)
            .foregroundColor(color ?? p.ink)
    }
}

extension View {
    func boxed(_ color: Color, _ width: CGFloat) -> some View {
        self.overlay(Rectangle().stroke(color, lineWidth: width))
    }
}
