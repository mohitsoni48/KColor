import SwiftUI
import shared

struct ContentView: View {
	let greet = Greeting().greet()

	var body: some View {
		Text(greet)
            .foregroundColor(.primary)
            .foregroundColor(getColor(kColor: KColorRes.shared.primary))

	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}



func getColor(kColor: String) -> Color {
    return Color(GetColorKt.getColor(kColor: kColor))
}
