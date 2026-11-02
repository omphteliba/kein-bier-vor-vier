import SwiftUI

/// A glass of beer drawn behind content: `top` above the foam (cream on the
/// phone, foam on the watch's full glass), a soft wavy band of foam at
/// `foamAt` points from the top, beer below with rising carbonation.
/// `phase` (0...1) moves the bubbles; pass a constant for a still glass.
struct BeerGlass: View {
    var foamAt: CGFloat
    var top: Color = .cream
    var foamScale: CGFloat = 1
    var phase: Double = 0.3

    var body: some View {
        Canvas { context, size in
            let foamY = min(foamAt, size.height)
            context.fill(Path(CGRect(x: 0, y: 0, width: size.width, height: foamY)), with: .color(top))
            let beer = CGRect(x: 0, y: foamY, width: size.width, height: size.height - foamY)
            context.fill(
                Path(beer),
                with: .linearGradient(
                    Gradient(colors: [.amber, .darkAmber]),
                    startPoint: CGPoint(x: 0, y: foamY),
                    endPoint: CGPoint(x: 0, y: size.height)
                )
            )
            drawBubbles(in: context, size: size, foamY: foamY)
            drawFoam(in: context, width: size.width, y: foamY)
        }
        .accessibilityHidden(true)
    }

    /// Gently wavy top and bottom edges, slightly out of phase, plus a few pores.
    private func drawFoam(in context: GraphicsContext, width: CGFloat, y: CGFloat) {
        let thickness = 18 * foamScale
        let amplitude = 4 * foamScale
        let wavelength = 120 * foamScale
        func edge(_ x: CGFloat, _ side: CGFloat, _ shift: CGFloat) -> CGFloat {
            y + side * thickness / 2 + amplitude * sin(x / wavelength * 2 * .pi + shift)
        }
        var path = Path()
        path.move(to: CGPoint(x: 0, y: edge(0, -1, 0)))
        for x in stride(from: CGFloat(0), through: width, by: 3) {
            path.addLine(to: CGPoint(x: x, y: edge(x, -1, 0)))
        }
        for x in stride(from: width, through: 0, by: -3) {
            path.addLine(to: CGPoint(x: x, y: edge(x, 1, 2.2)))
        }
        path.closeSubpath()
        context.fill(path, with: .color(.foamWhite))
        for (fx, fy) in [(0.10, 0.15), (0.27, -0.2), (0.46, 0.25), (0.64, -0.1), (0.83, 0.2), (0.95, -0.15)] {
            let x = width * fx
            let r = 1.6 * foamScale
            let cy = y + fy * thickness + amplitude * sin(x / wavelength * 2 * .pi + 1.1)
            context.fill(Path(ellipseIn: CGRect(x: x - r, y: cy - r, width: 2 * r, height: 2 * r)), with: .color(.foam))
        }
    }

    /// Small bubbles drifting up through the beer.
    private func drawBubbles(in context: GraphicsContext, size: CGSize, foamY: CGFloat) {
        let depth = size.height - foamY
        let bubbles: [(x: Double, offset: Double, speed: Double, r: Double)] = [
            (0.08, 0.00, 1.0, 3), (0.17, 0.55, 1.6, 2), (0.29, 0.20, 1.2, 4), (0.44, 0.75, 2.0, 2.5),
            (0.58, 0.35, 1.4, 3.5), (0.71, 0.90, 1.0, 2), (0.83, 0.10, 1.8, 3), (0.93, 0.62, 1.3, 2.5),
        ]
        for b in bubbles {
            let progress = (phase * b.speed + b.offset).truncatingRemainder(dividingBy: 1)
            let y = size.height - progress * depth
            let x = b.x * size.width + sin(progress * 12 + b.x * 20) * 3
            context.fill(
                Path(ellipseIn: CGRect(x: x - b.r, y: y - b.r, width: 2 * b.r, height: 2 * b.r)),
                with: .color(.white.opacity(0.35))
            )
        }
    }
}

/// The flag on a round white beer mat.
struct FlagCoaster: View {
    var flag: String
    var size: CGFloat = 92

    var body: some View {
        Text(flag)
            .font(.system(size: size * 0.54))
            .frame(width: size, height: size)
            .background(Circle().fill(.white))
            .overlay(Circle().stroke(Color.foam, lineWidth: size / 30))
            .shadow(color: .black.opacity(0.15), radius: size / 15, y: 2)
    }
}
