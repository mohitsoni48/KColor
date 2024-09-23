import SwiftUI
import shared

struct ContentView: View {
	let greet = Greeting().greet()

	var body: some View {
		Text(greet)
            .foregroundColor(.primary)
            .foregroundColor(getColor(kColorRes: KColorRes.primary()))

	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}



func getColor(kColorRes: KColorRes) -> Color {
    return Color(GetColorKt.getColor(kColorRes: kColorRes))
}
