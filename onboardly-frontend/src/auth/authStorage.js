export function clearStoredAuth() {
  localStorage.removeItem("jwt");
  localStorage.removeItem("user");
}

export function getStoredAuth() {
  const token = localStorage.getItem("jwt");
  const savedUser = localStorage.getItem("user");

  if (!token || !savedUser) {
    return {
      token: null,
      user: null,
    };
  }

  try {
    return {
      token,
      user: JSON.parse(savedUser),
    };
  } catch {
    clearStoredAuth();

    return {
      token: null,
      user: null,
    };
  }
}

export function saveStoredAuth(token, user) {
  localStorage.setItem("jwt", token);
  localStorage.setItem("user", JSON.stringify(user));
}

export function saveStoredUser(user) {
  localStorage.setItem("user", JSON.stringify(user));
}
