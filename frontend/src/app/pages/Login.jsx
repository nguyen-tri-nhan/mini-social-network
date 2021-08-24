import { useState, useEffect } from 'react';
import {
	Button,
	Checkbox,
	Container,
	CssBaseline,
	FormControlLabel,
	Grid,
	Link,
	TextField,
	Typography
} from '@material-ui/core';
import MAuth from '../model/MAuth'
import RouteConstants from '../routes/RouteConstants';

const Login = () => {

	document.title = 'Đăng nhập';


	const [usernameOrEmail, setUsernameOrEmail] = useState('');
	const [password, setPassword] = useState('');
	const [loginError, setLoginError] = useState(false);

	const onLoginError = () => {
		setLoginError(true);
	}

	const onSubmit = (e) => {
		MAuth.login({ usernameOrEmail, password }, onLoginError);
	}

	const renderLoginError = (show) => {
		return (
			show &&
			<Grid item xs>
				<Typography>Tài khoản hoặc mật khẩu không đúng</Typography>
			</Grid>
		);
	};

	const onUserNameChange = (e) => {
		setUsernameOrEmail(e.target.value);
	}

	const onPasswordChange = (e) => {
		setPassword(e.target.value);
	}
	return (
		<div>
			<Container component="main" maxWidth="xs">
				<CssBaseline />
				<div className="login-box">
					<Typography component="h1" variant="h5">
						Đăng nhập
					</Typography>
					<form className="login-form" noValidate>
						<TextField
							onChange={onUserNameChange}
							variant="outlined"
							margin="normal"
							required
							fullWidth
							id="username"
							label="Tên đăng nhập hoặc email"
							name="username"
							autoComplete="username"
							autoFocus
						/>
						<TextField
							onChange={onPasswordChange}
							variant="outlined"
							margin="normal"
							required
							fullWidth
							name="password"
							label="Mật khẩu"
							type="password"
							id="password"
							autoComplete="current-password"
						/>
						<FormControlLabel
							control={<Checkbox value="remember" color="primary" />}
							label="Ghi nhớ tài khoản"
						/>
						<Button
							// type="submit"
							onClick={onSubmit}
							fullWidth
							variant="contained"
							color="primary"
							className="btn-login"
						>
							Đăng nhập
						</Button>
						<Grid container>
							{renderLoginError(loginError)}
						</Grid>
						<Grid container>
							<Grid item xs>
								<Link href="#" variant="body2">
									Quên mật khẩu?
								</Link>
							</Grid>
							<Grid item>
								<Link href={RouteConstants.signup} variant="body2">
									{"Chưa có tài khoản? Đăng ký ngay"}
								</Link>
							</Grid>
						</Grid>
					</form>
				</div>
			</Container>
		</div>
	);
}

export default Login;