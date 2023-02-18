package me.scidev5.application.util

import io.ktor.util.*


object Env : EnvReader() {

    val sessionCookieSignKey = getRequired("sessionCookieSignKey") { hex(it) }
    /*
// Code for generating a new one, throw in any secure context webpage
(async () => {
  const key = await window.crypto.subtle.generateKey({
      name: "HMAC",
      hash: {
        name: "SHA-256"
      }
    },
    true,
    ["sign", "verify"]
  );
  const keyRaw = new Uint8Array(await crypto.subtle.exportKey("raw", key));
  console.log(
    [...keyRaw].flatMap(v => [
      Math.floor(v / 0x10),
      v % 0x10,
    ])
    .map(v => v.toString(16))
    .join("")
  );
})();
     */

}